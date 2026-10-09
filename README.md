# RetroAmp98

A retro Windows 98-style media player. Pick a file, hit play.

<picture>
  <source media="(prefers-color-scheme: dark)" srcset="https://github.com/user-attachments/assets/42771171-7d39-4d65-a98d-09b81ad0a8a8">
  <source media="(prefers-color-scheme: light)" srcset="https://github.com/user-attachments/assets/ec909c2a-1f77-4912-a40c-082d89ae8a09">
  <img alt="RetroAmp98 logo" src="https://github.com/user-attachments/assets/ec909c2a-1f77-4912-a40c-082d89ae8a09" width="200" height="50">
</picture>

## About RetroAmp98

**RetroAmp98** is a mini project I built for my **CSE 3rd semester Object-Oriented Programming with Java coursework**. It plays audio and video files from your own machine, in a window that looks like it came out of 1998.

The coursework asked for a small GUI app, and I built a music player. This is a fresh rebuild of that idea, since the original code never made it to GitHub. The interface is Swing with the Windows look and feel, JavaFX does the actual playback, and the code is organized around interfaces, strategies, and a state machine, because I wanted to practice OOP design properly.


## Tech Stack

| Tech | Usage |
|---|---|
| Java 21 | Core language |
| Swing | GUI, with the Windows look and feel |
| JavaFX Media | Audio and video playback engine |
| Maven | Build and dependency management |