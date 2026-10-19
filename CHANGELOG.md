# Changelog

All notable changes to this project are documented here. The format is based on
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

## [Unreleased]

### Added

- Offline maps: the map reads PMTiles region files from the app folder, with bundled
  fonts, a Regions screen to list and delete them, and `tools/tiles` to build a region
  from an OSM extract. The map no longer needs a network.
- Import a GPX file (tracks and routes) as saved tracks from the Tracks screen.
- A Re-centre button on the map appears after panning away from the position and
  resumes following.
- Initial project harness: issue and PR templates, contribution and security
  policies, docs structure (design, decisions, handoffs), and Claude Code
  working rules.
