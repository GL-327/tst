"""Synthesizes every Jujutsu Craft sound from oscillators, noise and envelopes.

All audio is original: nothing is sampled. Each recipe makes a mono 44.1 kHz signal (Minecraft
needs mono for positional sound), which ffmpeg encodes to Ogg Vorbis. The script also writes
assets/jjkcraft/sounds.json, with several pitch/timbre variants per event for variety.

Usage: python tools/gen_sounds.py <assets/jjkcraft directory>
Requires numpy and ffmpeg (with libvorbis) on PATH.
"""
import json
import zlib
import os
import subprocess
import sys
import tempfile
import wave

import numpy as np

SR = 44100


# ---------------------------------------------------------------- primitives

def t_axis(seconds):
    return np.arange(int(SR * seconds)) / SR


def sweep(f0, f1, seconds, curve=1.0):
    """Phase of a sine whose frequency moves from f0 to f1."""
    t = t_axis(seconds)
    k = (t / seconds) ** curve
    freq = f0 + (f1 - f0) * k
    return 2 * np.pi * np.cumsum(freq) / SR


def sine(f0, seconds, f1=None, curve=1.0):
    return np.sin(sweep(f0, f0 if f1 is None else f1, seconds, curve))


def saw(f0, seconds, f1=None):
    phase = sweep(f0, f0 if f1 is None else f1, seconds) / (2 * np.pi)
    return 2 * (phase - np.floor(phase + 0.5))


def square(f0, seconds, f1=None):
    return np.sign(sine(f0, seconds, f1))


def noise(seconds, rng):
    return rng.uniform(-1, 1, int(SR * seconds))


def lowpass(x, cutoff):
    """One-pole low-pass. cutoff may be a number or an array (Hz)."""
    cutoff = np.broadcast_to(np.asarray(cutoff, dtype=float), x.shape)
    a = np.exp(-2 * np.pi * cutoff / SR)
    y = np.empty_like(x)
    prev = 0.0
    for i in range(len(x)):
        prev = (1 - a[i]) * x[i] + a[i] * prev
        y[i] = prev
    return y


def highpass(x, cutoff):
    return x - lowpass(x, cutoff)


def bandpass(x, low, high):
    return lowpass(highpass(x, low), high)


def env_exp(seconds, decay):
    return np.exp(-t_axis(seconds) / decay)


def env_adsr(seconds, a, d, s, r):
    n = int(SR * seconds)
    env = np.zeros(n)
    na, nd, nr = int(SR * a), int(SR * d), int(SR * r)
    ns = max(0, n - na - nd - nr)
    idx = 0
    for seg, start, end in ((na, 0, 1), (nd, 1, s), (ns, s, s), (nr, s, 0)):
        seg = min(seg, n - idx)
        if seg > 0:
            env[idx:idx + seg] = np.linspace(start, end, seg, endpoint=False)
            idx += seg
    return env


def tremolo(seconds, rate, depth):
    return 1 - depth * (0.5 + 0.5 * np.sin(2 * np.pi * rate * t_axis(seconds)))


def drive(x, amount):
    return np.tanh(x * amount) / np.tanh(amount)


def reverb(x, room=0.35, decay=0.45, rng=None):
    """Small comb-filter reverb tail; keeps the dry signal and appends a tail."""
    tail = int(SR * room * 2)
    y = np.concatenate([x, np.zeros(tail)])
    for delay_ms, gain in ((29.7, decay), (37.1, decay * 0.9), (41.1, decay * 0.8), (43.7, decay * 0.7)):
        d = int(SR * delay_ms / 1000)
        out = np.zeros_like(y)
        for i in range(len(y)):
            out[i] = y[i] + (gain * out[i - d] if i >= d else 0)
        y = y + 0.25 * (out - y)
    return y


def pad(x, seconds):
    n = int(SR * seconds)
    if len(x) >= n:
        return x[:n]
    return np.concatenate([x, np.zeros(n - len(x))])


def mix(*parts):
    n = max(len(p) for p in parts)
    return sum(pad(p, n / SR) for p in parts)


def crackle(seconds, rng, density=60, length=0.004):
    """Sparse electric pops."""
    x = np.zeros(int(SR * seconds))
    for _ in range(int(density * seconds)):
        start = rng.integers(0, len(x))
        n = int(SR * length * rng.uniform(0.5, 1.5))
        burst = rng.uniform(-1, 1, n) * np.exp(-np.arange(n) / (n / 4))
        end = min(len(x), start + n)
        x[start:end] += burst[:end - start] * rng.uniform(0.3, 1.0)
    return x


def normalize(x, peak=0.89):
    m = np.max(np.abs(x))
    return x if m == 0 else x * (peak / m)


def fade(x, fade_in=0.004, fade_out=0.02):
    n_in, n_out = int(SR * fade_in), int(SR * fade_out)
    x = x.copy()
    if n_in:
        x[:n_in] *= np.linspace(0, 1, n_in)
    if n_out:
        x[-n_out:] *= np.linspace(1, 0, n_out)
    return x


# ---------------------------------------------------------------- recipes
# Each recipe takes (rng, variant) and returns a float array. Variants shift pitch and timbre.

def overexert(rng, v):
    d = 0.9
    p = 1 + 0.06 * (v - 1)
    hum = sine(95 * p, d, 55 * p, curve=0.6) * tremolo(d, 11 + v, 0.6)
    grind = lowpass(saw(48 * p, d, 38 * p), 600) * 0.5
    hiss = bandpass(noise(d, rng), 900, 3500) * env_exp(d, 0.18) * 0.6
    pops = crackle(d, rng, density=40) * 0.35
    return reverb(drive(mix(hum, grind, hiss, pops) * env_adsr(d, 0.01, 0.25, 0.55, 0.5), 2.2), room=0.2)


def promote(rng, v):
    d = 2.4
    base = [261.6, 329.6, 392.0, 523.3][v % 4] if v < 4 else 261.6
    parts = []
    for i, ratio in enumerate((1, 1.25, 1.5, 2.0)):
        start = int(SR * i * 0.12)
        tone = (sine(base * ratio, d) + 0.4 * sine(base * ratio * 2.01, d)) * env_exp(d, 0.9)
        parts.append(np.concatenate([np.zeros(start), tone])[: int(SR * d)])
    shimmer = highpass(noise(d, rng), 6000) * env_adsr(d, 0.3, 0.6, 0.2, 1.4) * 0.15
    gong = sine(base / 2, d) * env_exp(d, 1.2) * 0.5
    return reverb(mix(*parts, shimmer, gong), room=0.45)


def ui_click(rng, v):
    d = 0.07
    return sine(1800 + 120 * v, d, 900) * env_exp(d, 0.012) + bandpass(noise(d, rng), 2000, 8000) * env_exp(d, 0.004) * 0.4


def ui_open(rng, v):
    d = 0.45
    sweep_tone = sine(220 + 20 * v, d, 660 + 30 * v, curve=0.5) * env_adsr(d, 0.02, 0.1, 0.6, 0.3)
    brush = bandpass(noise(d, rng), 400, 2500) * env_adsr(d, 0.05, 0.2, 0.3, 0.2) * 0.5
    return reverb(mix(sweep_tone * 0.6, brush), room=0.2)


def whoosh(rng, v, d=0.45, lo=300, hi=3000):
    n = noise(d, rng)
    cutoff = np.linspace(lo, hi, len(n)) * (1 + 0.08 * v)
    cutoff = np.concatenate([cutoff[: len(n) // 2], cutoff[len(n) // 2:][::-1]])
    return lowpass(n, cutoff) * env_adsr(d, d * 0.35, d * 0.2, 0.5, d * 0.4)


def cast(rng, v):
    d = 0.5
    return reverb(mix(whoosh(rng, v, d), sine(330 + 40 * v, d, 880) * env_adsr(d, 0.05, 0.2, 0.3, 0.2) * 0.25), room=0.15)


def impact(rng, v, weight=1.0):
    d = 0.6
    thud = sine(90 / weight + 6 * v, d, 40) * env_exp(d, 0.09 * weight)
    crack = bandpass(noise(d, rng), 1200, 7000) * env_exp(d, 0.015) * 0.8
    body = lowpass(noise(d, rng), 400) * env_exp(d, 0.06) * 0.7
    return drive(mix(thud * 1.2, crack, body), 2.0)


def strike(rng, v):
    return impact(rng, v, 1.0)


def black_flash(rng, v):
    d = 1.4
    swell = lowpass(noise(d, rng), np.linspace(200, 6000, int(SR * d))) * np.linspace(0, 1, int(SR * d)) ** 3 * 0.5
    swell = pad(swell[: int(SR * 0.35)], d)
    hit = np.concatenate([np.zeros(int(SR * 0.35)), impact(rng, v, 1.6)])
    zap = np.concatenate([np.zeros(int(SR * 0.35)), crackle(1.0, rng, density=220, length=0.006) * env_exp(1.0, 0.35)])
    sub = np.concatenate([np.zeros(int(SR * 0.35)), sine(55, 1.0, 30) * env_exp(1.0, 0.4)])
    return reverb(drive(mix(swell, hit * 1.3, zap * 0.9, sub), 2.5), room=0.35)


def bolt_fire(rng, v):
    d = 0.4
    tone = square(440 + 50 * v, d, 220) * env_exp(d, 0.08) * 0.25
    return mix(whoosh(rng, v, d, 800, 5000), lowpass(tone, 3000))


def bolt_hit(rng, v):
    return impact(rng, v, 0.8) * 0.9


def slash(rng, v):
    d = 0.55
    ring = sum(sine(f * (1 + 0.03 * v), d) * env_exp(d, 0.25) for f in (1900, 2870, 4100)) * 0.12
    return mix(whoosh(rng, v, 0.3, 1500, 9000), ring)


def attract(rng, v):
    d = 1.0
    suck = lowpass(noise(d, rng), np.linspace(6000, 200, int(SR * d))) * np.linspace(0.2, 1, int(SR * d))
    tone = sine(800 + 30 * v, d, 120, curve=0.4) * env_adsr(d, 0.1, 0.3, 0.6, 0.3) * 0.5
    return reverb(mix(suck * 0.7, tone), room=0.3)


def repel(rng, v):
    d = 1.0
    boom = sine(70 + 5 * v, d, 35) * env_exp(d, 0.25)
    blast = lowpass(noise(d, rng), np.linspace(5000, 300, int(SR * d))) * env_exp(d, 0.2)
    return reverb(drive(mix(boom * 1.2, blast), 2.0), room=0.4)


def merge(rng, v):
    d = 2.6
    hum = mix(sine(110, d, 220), sine(165, d, 330) * 0.6) * np.linspace(0, 1, int(SR * d)) ** 2 * 0.5
    hum = pad(hum[: int(SR * 1.2)], d)
    blast = np.concatenate([np.zeros(int(SR * 1.2)), repel(rng, v)])
    shimmer = np.concatenate([np.zeros(int(SR * 1.2)), highpass(noise(1.4, rng), 5000) * env_exp(1.4, 0.5) * 0.3])
    return reverb(mix(hum, blast * 1.2, shimmer), room=0.5)


def speech(rng, v):
    """A synthetic formant burst (not a voice): buzzy source through vowel-like band filters."""
    d = 0.7
    src = saw(110 + 15 * v, d, 90)
    formants = bandpass(src, 600, 1100) + 0.7 * bandpass(src, 1700, 2600) + 0.4 * bandpass(src, 2900, 3600)
    return reverb(drive(formants * env_adsr(d, 0.01, 0.15, 0.7, 0.3), 3.0), room=0.5)


def heal(rng, v):
    d = 1.8
    base = 523.3 * (1 + 0.04 * v)
    bells = sum(sine(base * r, d) * env_exp(d, 0.7 / r) for r in (1, 2.76, 5.4)) * 0.3
    air = highpass(noise(d, rng), 7000) * env_adsr(d, 0.4, 0.5, 0.2, 0.8) * 0.12
    return reverb(mix(bells, air), room=0.5)


def chain(rng, v):
    d = 0.8
    out = np.zeros(int(SR * d))
    for i in range(7 + v):
        start = int(SR * (0.03 + i * 0.075 + rng.uniform(-0.01, 0.01)))
        clink = sum(sine(f * rng.uniform(0.95, 1.05), 0.15) for f in (2400, 3700, 5300)) * env_exp(0.15, 0.03)
        end = min(len(out), start + len(clink))
        out[start:end] += clink[: end - start] * rng.uniform(0.4, 0.9)
    return reverb(out * 0.4, room=0.2)


def barrier(rng, v):
    d = 1.2
    glass = sum(sine(f * (1 + 0.02 * v), d) for f in (660, 990, 1320)) * env_adsr(d, 0.05, 0.3, 0.5, 0.5) * 0.25
    hum = sine(110, d) * tremolo(d, 6, 0.5) * env_adsr(d, 0.1, 0.2, 0.6, 0.5) * 0.4
    return reverb(mix(glass, hum), room=0.4)


def summon(rng, v):
    d = 1.6
    rumble = lowpass(noise(d, rng), 180) * env_adsr(d, 0.4, 0.4, 0.6, 0.6) * 1.5
    growl = lowpass(saw(55 + 5 * v, d, 42), 500) * env_adsr(d, 0.3, 0.4, 0.5, 0.6) * 0.5
    return reverb(drive(mix(rumble, growl), 1.8), room=0.45)


def domain_open(rng, v):
    d = 4.0
    drone = mix(saw(41.2, d), saw(41.2 * 1.498, d) * 0.6, saw(41.2 * 2.003, d) * 0.4)
    drone = lowpass(drone, np.linspace(150, 2400, int(SR * d))) * env_adsr(d, 1.2, 0.8, 0.7, 1.5) * 0.6
    gong = np.concatenate([np.zeros(int(SR * 1.0)), mix(*(sine(f, 3.0) * env_exp(3.0, 1.6 / (i + 1)) for i, f in enumerate((73.4, 110.5, 197.3, 311.0))))])
    rise = highpass(noise(d, rng), 3000) * np.linspace(0, 1, int(SR * d)) ** 4 * 0.2
    return reverb(drive(mix(drone, gong * 0.8, rise), 1.6), room=0.6)


def domain_ambient(rng, v):
    d = 6.0
    t = t_axis(d)
    base = 55 * (1 + 0.05 * v)
    # Partials chosen to repeat cleanly over 6 s so the loop has no click.
    drone = sum(np.sin(2 * np.pi * round(base * r * d) / d * t) * a for r, a in ((1, 0.5), (1.5, 0.3), (2, 0.2), (3.01, 0.1)))
    breath = lowpass(noise(d, rng), 500) * (0.5 + 0.5 * np.sin(2 * np.pi * t / d)) * 0.3
    return mix(drone, breath)


def domain_close(rng, v):
    d = 2.0
    x = domain_open(rng, v)[: int(SR * d)][::-1]
    return x * env_adsr(d, 0.01, 0.3, 0.7, 0.6)


def domain_clash(rng, v):
    d = 1.5
    shatter = sum(sine(f * rng.uniform(0.97, 1.03), d) * env_exp(d, 0.4 / (i + 1)) for i, f in enumerate((1480, 2210, 3170, 4400))) * 0.2
    return reverb(drive(mix(impact(rng, v, 1.8), shatter, crackle(d, rng, 150) * env_exp(d, 0.5) * 0.5), 2.2), room=0.5)


def curse_spawn(rng, v):
    d = 1.4
    gurgle = bandpass(noise(d, rng), 200, 900) * tremolo(d, 9 + v, 0.8) * env_adsr(d, 0.3, 0.3, 0.6, 0.5)
    return reverb(mix(gurgle, summon(rng, v)[: int(SR * d)] * 0.6), room=0.3)


def curse_hurt(rng, v):
    d = 0.35
    screech = bandpass(saw(380 + 60 * v, d, 210), 500, 3000) * env_exp(d, 0.1)
    return drive(mix(screech, bandpass(noise(d, rng), 800, 4000) * env_exp(d, 0.05) * 0.6), 2.5)


def curse_death(rng, v):
    d = 1.6
    hiss = highpass(noise(d, rng), 2000) * env_adsr(d, 0.02, 0.4, 0.4, 1.0) * 0.6
    fall = bandpass(saw(300 + 30 * v, d, 60), 200, 2500) * env_exp(d, 0.4) * 0.6
    return reverb(mix(hiss, fall, crackle(d, rng, 80) * env_exp(d, 0.6) * 0.4), room=0.4)


def curse_ambient(rng, v):
    d = 1.2
    murmur = bandpass(saw(70 + 10 * v, d, 60), 150, 700) * tremolo(d, 4 + v, 0.7) * env_adsr(d, 0.3, 0.2, 0.7, 0.4)
    return reverb(murmur * 0.8, room=0.35)


def curse_toll(rng, v):
    # A cracked bell: inharmonic partials, one of them buzzing where the crack is.
    d = 2.6
    p = 1 + 0.05 * (v - 1)
    partials = ((196, 1.0), (233, 0.5), (412, 0.6), (590, 0.35), (871, 0.25), (1243, 0.15))
    body = sum(sine(f * p * rng.uniform(0.995, 1.005), d) * env_exp(d, 0.9 / (1 + i * 0.4)) * a for i, (f, a) in enumerate(partials))
    crack = bandpass(saw(412 * p, d), 300, 1800) * tremolo(d, 31, 0.9) * env_exp(d, 0.35) * 0.25
    clang = bandpass(noise(d, rng), 1500, 6000) * env_exp(d, 0.03) * 0.5
    return reverb(mix(body, crack, clang), room=0.55, decay=0.6)


def curse_roar(rng, v):
    d = 1.5
    p = 1 + 0.07 * (v - 1)
    throat = lowpass(saw(62 * p, d, 44 * p), 900) * env_adsr(d, 0.12, 0.3, 0.75, 0.6)
    growl = bandpass(noise(d, rng), 120, 1100) * tremolo(d, 23, 0.8) * env_adsr(d, 0.08, 0.3, 0.6, 0.7) * 0.8
    return reverb(drive(mix(throat, growl), 2.8), room=0.4)


# name: (recipe, variants, subtitle key or None)
SOUNDS = {
    "energy.overexert": (overexert, 2, "subtitles.jjkcraft.energy.overexert"),
    "grade.promote": (promote, 1, "subtitles.jjkcraft.grade.promote"),
    "ui.click": (ui_click, 2, None),
    "ui.open": (ui_open, 2, None),
    "technique.cast": (cast, 3, "subtitles.jjkcraft.technique.cast"),
    "technique.strike": (strike, 3, "subtitles.jjkcraft.technique.strike"),
    "technique.black_flash": (black_flash, 2, "subtitles.jjkcraft.technique.black_flash"),
    "technique.bolt_fire": (bolt_fire, 3, "subtitles.jjkcraft.technique.bolt_fire"),
    "technique.bolt_hit": (bolt_hit, 2, "subtitles.jjkcraft.technique.bolt_hit"),
    "technique.slash": (slash, 3, "subtitles.jjkcraft.technique.slash"),
    "technique.attract": (attract, 2, "subtitles.jjkcraft.technique.attract"),
    "technique.repel": (repel, 2, "subtitles.jjkcraft.technique.repel"),
    "technique.merge": (merge, 1, "subtitles.jjkcraft.technique.merge"),
    "technique.speech": (speech, 3, "subtitles.jjkcraft.technique.speech"),
    "technique.heal": (heal, 2, "subtitles.jjkcraft.technique.heal"),
    "technique.chain": (chain, 2, "subtitles.jjkcraft.technique.chain"),
    "technique.barrier": (barrier, 2, "subtitles.jjkcraft.technique.barrier"),
    "technique.summon": (summon, 2, "subtitles.jjkcraft.technique.summon"),
    "domain.open": (domain_open, 1, "subtitles.jjkcraft.domain.open"),
    "domain.ambient": (domain_ambient, 1, None),
    "domain.close": (domain_close, 1, "subtitles.jjkcraft.domain.close"),
    "domain.clash": (domain_clash, 1, "subtitles.jjkcraft.domain.clash"),
    "curse.spawn": (curse_spawn, 2, "subtitles.jjkcraft.curse.spawn"),
    "curse.hurt": (curse_hurt, 3, "subtitles.jjkcraft.curse.hurt"),
    "curse.death": (curse_death, 2, "subtitles.jjkcraft.curse.death"),
    "curse.ambient": (curse_ambient, 3, "subtitles.jjkcraft.curse.ambient"),
    "curse.toll": (curse_toll, 2, "subtitles.jjkcraft.curse.toll"),
    "curse.roar": (curse_roar, 2, "subtitles.jjkcraft.curse.roar"),
}


def write_ogg(signal, path):
    signal = fade(normalize(signal))
    pcm = (signal * 32767).astype(np.int16)
    with tempfile.TemporaryDirectory() as tmp:
        wav_path = os.path.join(tmp, "s.wav")
        with wave.open(wav_path, "wb") as w:
            w.setnchannels(1)
            w.setsampwidth(2)
            w.setframerate(SR)
            w.writeframes(pcm.tobytes())
        os.makedirs(os.path.dirname(path), exist_ok=True)
        subprocess.run(["ffmpeg", "-y", "-loglevel", "error", "-i", wav_path, "-c:a", "libvorbis", "-q:a", "4", path], check=True)


def main(assets_dir, only=None):
    sounds_json = {}
    for name, (recipe, variants, subtitle) in SOUNDS.items():
        files = []
        for v in range(1, variants + 1):
            file_id = name.replace(".", "/") + ("" if variants == 1 else f"_{v}")
            if only is None or name in only:
                rng = np.random.default_rng(zlib.crc32(f"{name}:{v}".encode()))
                write_ogg(recipe(rng, v), os.path.join(assets_dir, "sounds", file_id + ".ogg"))
            files.append({"name": "jjkcraft:" + file_id, "stream": name == "domain.ambient"})
        entry = {"sounds": files}
        if subtitle:
            entry["subtitle"] = subtitle
        sounds_json[name] = entry
        print("ok", name)

    with open(os.path.join(assets_dir, "sounds.json"), "w", encoding="utf-8") as f:
        json.dump(sounds_json, f, indent=2)
        f.write("\n")


if __name__ == "__main__":
    main(sys.argv[1], set(sys.argv[2:]) or None)
