# Change Log
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](http://keepachangelog.com/)
and this project adheres to [Semantic Versioning](http://semver.org/).

## [Unreleased]

### Added
- Sharing links and messages into Qabel to send them as a chat message

### Changed
- chat: visual feedback on send button

### Fixed

## [0.14.0] - 2016-12

### Added
- Remote folder chooser activity
- Share into Qabel from other Android apps (SEND-Intent)
- Open shared files from the chat

### Changed

### Fixed

## [0.13.2] - 2016-12-12

### Added
- Remote folder chooser activity (unused currently)

### Changed
- Box operations running and tracked in background

### Fixed
- Guard against unusable address book entries to prevent crashes
- Fix crash on Android 7 when receiving or sending a file share.

## [0.13.0] - 2016-11-1
### Added

### Changed
- beautify chat with contact avatar and colored msg items

### Fixed
- fix keyboard handling with emoji chooser

## [0.12.1] - 2016-10-20
### Fixed
- Downgrade SDK to 23 because the NDK is buggy in 24

## [0.12.0] - 2016-10-20
### Added
- Landscape Mode
- Mark all messages as read action in conversations list
- Extra emoji button in chat
- New Emoji font

### Changed
- Multiline chat improved
- Crash reporting from background services enabled

## [0.11.2] - 2016-10-11
### Fixed
- Android 7 compatibility
- Fixed chat notifications
- Scroll chat to bottom on refresh messages
