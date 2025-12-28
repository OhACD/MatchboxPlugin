# Matchbox Wiki Setup Guide

This guide explains how to set up the GitHub Wiki for this repository.

## Wiki Content Location

All wiki-ready content is located in the `wiki-pages/` directory. These files are designed to be copied directly into the GitHub Wiki.

## Setting Up the Wiki

### Option 1: Manual Setup (Recommended)
1. Navigate to the repository Wiki tab on GitHub
2. Create a new page for each file in `wiki-pages/`
3. Copy and paste the content from the corresponding `.md` file
4. Use the filename (without `.md`) as the page title

### Option 2: Bulk Import
The `wiki-pages/` directory can be cloned as a Git repository (GitHub wikis are Git repos):
```bash
git clone https://github.com/OhACD/MatchboxPlugin.wiki.git
cp wiki-pages/* MatchboxPlugin.wiki/
cd MatchboxPlugin.wiki
git add .
git commit -m "Update wiki pages"
git push
```

## Wiki Structure

The wiki is organized as follows:

- **Home.md** — Landing page with quick links
- **Getting-Started.md** — Installation and setup instructions
- **Commands.md** — Complete command reference
- **Configuration.md** — Configuration file guide
- **API.md** — API overview (links to MatchboxAPI_Docs.md)
- **Contributing.md** — Contribution guidelines
- **Changelog.md** — Release history (links to CHANGELOG.md)
- **FAQ.md** — Frequently asked questions
- **Developer-Notes.md** — Developer reference
- **Roadmap.md** — Future plans
- **_Sidebar.md** — Wiki navigation sidebar

## Maintaining Documentation

- **Source of Truth**: The `docs/` directory contains the canonical documentation for the repository
- **Wiki Staging**: The `wiki-pages/` directory contains wiki-ready versions with appropriate formatting
- **When updating**: Update both `docs/` and `wiki-pages/` to keep them in sync
- **For detailed API docs**: Keep `MatchboxAPI_Docs.md` in the repository root (too detailed for wiki)
