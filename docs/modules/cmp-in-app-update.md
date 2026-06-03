---
title: "cmp-in-app-update"
description: "Android in-app updates (immediate + flexible) with no-op fallbacks on other platforms."
---

# cmp-in-app-update

!!! info "README in progress"
    A canonical `cmp-in-app-update/README.md` hasn't been authored yet. Until it
    lands, see the [GitHub source](https://github.com/MobileByteLabs/KmpToolkit/tree/development/cmp-in-app-update)
    for the live code + KDoc, and the **Module reference** below for the
    contributor-facing developer notes.

    Want to help? Open a PR adding `cmp-in-app-update/README.md` and convert this
    page to the README-embedded flavor (see other module pages for the
    pattern).


## Module reference
{%
   include-markdown "../../cmp-in-app-update/DEVELOPMENT.md"
   start="## §1"
   end="## §3"
   heading-offset=1
   comments=false
%}

## API reference

Each release ships the module's full Dokka HTML site inside its
[`-javadoc.jar`](https://repo1.maven.org/maven2/io/github/mobilebytelabs/cmp-in-app-update/) artifact on Maven Central.

In IntelliJ / Android Studio the IDE mounts the jar and surfaces it
automatically in hover popups, Quick Documentation, and Symbol search.
