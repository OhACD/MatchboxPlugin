# Matchbox Documentation Guide

This document explains the documentation structure for the Matchbox plugin.

## Documentation Structure

The Matchbox project maintains documentation in three primary locations:

### 1. Repository Root (`/`)
Core project documentation files:

- **README.md** — Main project overview and quick start
- **CHANGELOG.md** — Complete release history
- **MatchboxAPI_Docs.md** — Comprehensive API documentation with examples
- **DEVELOPMENT_POLICY.md** — Project development approach and stability guarantees
- **WIKI.md** — Guide for setting up the GitHub Wiki

### 2. Documentation Directory (`/docs`)
User-facing documentation for server owners and developers:

- **README.md** — Documentation index and overview
- **GettingStarted.md** — Installation and setup guide
- **Commands.md** — Complete command reference
- **Configuration.md** — Configuration file guide
- **API.md** — API quick start for developers
- **Contributing.md** — Contribution guidelines

**Purpose**: These files are the canonical source for user documentation and are linked from the main README.

### 3. Wiki Pages Directory (`/wiki-pages`)
Wiki-ready markdown files for the GitHub Wiki:

- **Home.md** — Wiki landing page
- **Getting-Started.md** — Installation guide (wiki format)
- **Commands.md** — Commands reference (wiki format)
- **Configuration.md** — Config guide (wiki format)
- **API.md** — API documentation (wiki format)
- **FAQ.md** — Frequently asked questions
- **Developer-Notes.md** — Developer reference
- **Roadmap.md** — Project roadmap
- **Changelog.md** — Release history overview
- **Contributing.md** — Contribution guide (wiki format)
- **_Sidebar.md** — Wiki navigation sidebar
- **README.md** — Instructions for using wiki-pages

**Purpose**: Ready-to-paste content for the GitHub Wiki. These files have wiki-appropriate formatting and internal links.

## Documentation Maintenance

### When to Update Which Files

**For feature changes or bug fixes:**
1. Update CHANGELOG.md (repository root)
2. Update relevant files in docs/ if user-facing changes
3. Sync changes to wiki-pages/ for wiki consistency

**For API changes:**
1. Update MatchboxAPI_Docs.md (repository root)
2. Update docs/API.md if it affects the quick start
3. Update wiki-pages/API.md to match

**For configuration changes:**
1. Update docs/Configuration.md
2. Sync to wiki-pages/Configuration.md

**For command changes:**
1. Update docs/Commands.md
2. Sync to wiki-pages/Commands.md

### Keeping Documentation in Sync

The docs/ and wiki-pages/ directories should generally contain the same information, but formatted differently:

- **docs/** — Repository documentation (internal links to other repo files)
- **wiki-pages/** — Wiki documentation (internal links to other wiki pages)

Use this command to sync after making changes to docs/:
```bash
cp docs/GettingStarted.md wiki-pages/Getting-Started.md
cp docs/Commands.md wiki-pages/Commands.md
cp docs/Configuration.md wiki-pages/Configuration.md
cp docs/API.md wiki-pages/API.md
cp docs/Contributing.md wiki-pages/Contributing.md
```

Remember to adjust internal links when syncing (repository links vs wiki links).

## Documentation Best Practices

### Writing Style
- Use clear, concise language
- Include code examples where appropriate
- Provide links to related documentation
- Keep sections focused and well-organized

### Formatting
- Use proper Markdown syntax
- Include proper headings hierarchy (H1 → H2 → H3)
- Use code blocks with language syntax highlighting
- Use tables for structured data
- Use lists for step-by-step instructions

### Links
- **In docs/**: Use relative links to repository files (e.g., `[CHANGELOG](../CHANGELOG.md)`)
- **In wiki-pages/**: Use wiki-style links (e.g., `[Changelog](Changelog)`)
- Always verify links work after changes

### Code Examples
- Keep examples simple and focused
- Include comments explaining non-obvious code
- Show both the setup and usage
- Use realistic variable names

## For Contributors

When contributing documentation:

1. **Read existing docs** to understand the style and format
2. **Update all relevant locations** (docs/ and wiki-pages/ if applicable)
3. **Test all links** to ensure they work
4. **Follow the existing structure** and formatting
5. **Update CHANGELOG.md** if the change is user-visible
6. **Request review** for significant documentation changes

## Questions?

If you have questions about documentation:
- Check this guide first
- Look at existing documentation for examples
- Ask in Discord: https://discord.gg/BTDP3APfq8
- Open an issue for documentation improvements

---

**Last Updated**: 2025-12-28 (v0.9.5)
