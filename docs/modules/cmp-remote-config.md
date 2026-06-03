---
title: "cmp-remote-config"
description: "Firebase Remote Config wrapper for Kotlin Multiplatform."
---

# cmp-remote-config

!!! info "README in progress"
    A canonical `cmp-remote-config/README.md` hasn't been authored yet. Until it
    lands, see the [GitHub source](https://github.com/MobileByteLabs/KmpToolkit/tree/development/cmp-remote-config)
    for the live code + KDoc, and the **Module reference** below for the
    contributor-facing developer notes.

    Want to help? Open a PR adding `cmp-remote-config/README.md` and convert this
    page to the README-embedded flavor (see other module pages for the
    pattern).


## Module reference
{%
   include-markdown "../../cmp-remote-config/DEVELOPMENT.md"
   start="## §1"
   end="## §3"
   heading-offset=1
   comments=false
%}

## API reference

Each release ships the module's full Dokka HTML site inside its
[`-javadoc.jar`](https://repo1.maven.org/maven2/io/github/mobilebytelabs/cmp-remote-config/) artifact on Maven Central.

In IntelliJ / Android Studio the IDE mounts the jar and surfaces it
automatically in hover popups, Quick Documentation, and Symbol search.
