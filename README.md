# RetroAmp98

A small retro music player with a Windows Media Player 8/9 look. Pick a few songs, hit play.

<picture>
  <source media="(prefers-color-scheme: dark)" srcset="https://github.com/user-attachments/assets/42771171-7d39-4d65-a98d-09b81ad0a8a8">
  <source media="(prefers-color-scheme: light)" srcset="https://github.com/user-attachments/assets/ec909c2a-1f77-4912-a40c-082d89ae8a09">
  <img alt="RetroAmp98 logo" src="https://github.com/user-attachments/assets/ec909c2a-1f77-4912-a40c-082d89ae8a09" width="200" height="50">
</picture>

## About RetroAmp98

**RetroAmp98** is a mini project I built for my **CSE 3rd semester Object-Oriented Programming with Java coursework**. It plays audio files from your own machine (MP3, WAV, M4A) in a window that looks like it came out of the early 2000s.

The engine is separated from the UI, so the player logic doesn't know anything about Swing, and the playlist logic is tested on its own.

## Features

- Play, pause, stop, next, previous, seek, volume, and mute
- Playlist with add, remove, clear, double-click to play
- Repeat (Off / One / All) and shuffle
- Remembers volume, repeat, shuffle, window size, and your last playlist

## Tech Stack

| Tech | Usage |
|---|---|
| Java 21 | Core language |
| Swing | GUI, with the Windows look and feel |
| JavaFX Media | Audio playback engine |
| Maven | Build and dependency management |
| JUnit 5 | Tests |

## Running It

```bash
mvn exec:java -Dexec.mainClass="io.github.nikhilvirdi.retroamp98.RetroAmpApp"
```