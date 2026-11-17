# RetroAmp98

A retro arcade-style music player. Pick a few songs, hit play, and listen like it's 1998.

\<picture>   \<source media="(prefers-color-scheme: dark)" srcset="[https://github.com/user-attachments/assets/42771171-7d39-4d65-a98d-09b81ad0a8a8](https://github.com/user-attachments/assets/42771171-7d39-4d65-a98d-09b81ad0a8a8)">   \<source media="(prefers-color-scheme: light)" srcset="[https://github.com/user-attachments/assets/ec909c2a-1f77-4912-a40c-082d89ae8a09](https://github.com/user-attachments/assets/ec909c2a-1f77-4912-a40c-082d89ae8a09)">   \<img alt="RetroAmp98 logo" src="[https://github.com/user-attachments/assets/ec909c2a-1f77-4912-a40c-082d89ae8a09](https://github.com/user-attachments/assets/ec909c2a-1f77-4912-a40c-082d89ae8a09)" width="200" height="50"> \</picture>

### About RetroAmp98

**RetroAmp98** is a mini desktop music player I originally built with a friend for our **CSE 3rd semester Object-Oriented Programming with Java coursework**. It plays audio files from your own machine (MP3, WAV, M4A), with the usual playback controls, playlists, shuffle, repeat, and a recently played history.

The original version used Swing and took inspiration from Windows Media Player 8/9. I later rebuilt the interface in JavaFX, replacing the old Windows-style panels with a pixel-art arcade look: bitmap fonts, a black-and-blue color scheme, orange selection highlights, album artwork, and a small audio spectrum that actually responds to the music. The idea was to keep the feel of an old computer music player without just copying one.

The player also reads metadata directly from audio files, so properly tagged songs show their title, artist, album, artwork, and description instead of a messy filename. The playback engine, playlist logic, and interface are kept separate. That started as an OOP coursework decision, but it also made replacing the entire UI possible without throwing away the underlying player.

### How It Works

#### Playback Engine

JavaFX Media handles the actual audio playback. The engine is responsible for loading tracks, playing, pausing, stopping, seeking, controlling volume, and reporting playback state and duration.

The interface doesn't control the media engine directly. User actions go through `PlayerController`, which coordinates playback and playlist navigation, while listeners carry state changes back to the UI. That keeps the player logic independent of the buttons, progress bar, and other visual components.

Playback updates are synchronized with the JavaFX Application Thread, so the timeline, track information, and control states stay in sync with the music.

#### Playlist Management

The playlist keeps an ordered collection of tracks, with add, remove, reorder, clear, and double-click-to-play operations. M3U import and export let you save a playlist and open it again later without copying the actual audio files.

Shuffle and sequential playback use separate playback-order strategies. Repeat has three modes: Off, One, and All. Keeping these rules outside the interface means the same navigation logic works regardless of how the user starts a track.

#### Music Metadata

The player reads embedded audio metadata instead of depending on filenames. Jaudiotagger handles metadata extraction, including ID3 tags in MP3 files, while the interface displays the available information.

The Now Playing section shows the song title, artist, album, track number, duration, and embedded album cover. It also uses the audio file's `COMMENT` field for a short description when one is available. Missing tags fall back to sensible defaults rather than leaving broken labels or artwork.

File importing and metadata processing run in the background, so adding a folder of songs doesn't have to block the interface.

#### Listening History and Persistence

The playlist and listening history are two different things. The playlist contains the songs you've added, while Recently Played records tracks that actually started playing, with the newest entries first.

Both survive application restarts. RetroAmp98 also remembers the last playlist, volume, mute, shuffle, repeat, and window settings. If a previously imported file is missing, the player handles it without modifying the original music collection.

### Features

#### 1. Retro Arcade Interface

The entire interface uses JavaFX, FXML, and CSS, with pixel-style typography, a custom RetroAmp98 wordmark, electric-blue accents, orange playlist highlights, and compact playback controls. The layout includes a Now Playing section, album artwork, a seek bar, and a track list that switches between Playlist and Recently Played.

#### 2. Playback Controls

Play, pause, stop, previous, next, seeking, volume up/down, mute, shuffle, and repeat are available from the footer. The progress bar follows the current track and lets you jump to a different position. Shuffle changes the playback order, while repeat controls what happens when a track finishes.

#### 3. Music Library and Playlists

Import individual audio files, select multiple songs, add entire folders, or drag files directly into the window. The playlist supports reordering, removing tracks, and clearing the list. You can also import and export M3U playlists.

#### 4. Album Artwork and Track Information

Song information comes from the audio file's embedded metadata. Album covers appear beside the title, artist, album details, and description. Missing artwork or incomplete tags use fallbacks, so the interface still works with files that haven't been properly tagged.

#### 5. Recently Played

Keeps a chronological history of played tracks, separate from the active playlist. You can switch between the two views, replay a previously played song, and clear the history. The list is restored when the application opens again.

#### 6. Audio Visualizer

A small pixel-bar spectrum sits beside the playback timeline. It uses JavaFX Media's audio-spectrum data to react to the actual music rather than displaying a looping animation.

#### 7. Keyboard Shortcuts and Preferences

Space toggles playback, the arrow keys handle seeking and volume, `Ctrl+O` opens audio files, and `Ctrl+Shift+O` opens a folder. Volume, mute, playback modes, window position, playlists, and listening history are saved locally and restored across sessions.

### Tech Stack

| Tech         | Usage                                                                   |
| ------------ | ----------------------------------------------------------------------- |
| Java 21      | Core language, player logic, and application controllers                |
| JavaFX       | Native desktop interface and audio playback                             |
| FXML         | Interface layouts and components                                        |
| JavaFX CSS   | Retro styling, typography, and control states                           |
| Jaudiotagger | Embedded audio metadata and album-art extraction                        |
| Maven        | Build and dependency management                                         |
| JUnit 5      | Tests for playback logic, playlists, metadata, history, and persistence |

### Running It

Requires Java 21 and Maven.

```bash
mvn javafx:run
```

Or launch through the application's main class:

```bash
mvn compile
mvn exec:java -Dexec.mainClass="io.github.nikhilvirdi.retroamp98.RetroAmpApp"
```

Run the tests with:

```bash
mvn test
```

RetroAmp98 plays music stored on your machine. It doesn't stream songs or include a bundled music library; you'll need to import your own audio files.
