# Story scripts (`*.story`)

One file per story sequence, loaded by `story.StoryScriptLoader` (file name = sequence id, e.g.
`opening_intro.story`). UTF-8, one entry per line. Add new sequences in `StorySequences.java`.

```
# comment
speaker id = Display Name | portrait/path.png | #RRGGBB     (portrait "-" = none)

[bg backgrounds/vn_x.png | cast id1, id2]   settings for the NEXT dialogue block (all optional)
[overlay]                                   bottom-of-screen box instead of visual novel mode
id: Spoken line.                            consecutive lines of one speaker = one block
id: Another line.
---                                         force a new block for the same speaker
!wait 120                                   pause, in frames
!action name                                run game logic bound in StorySequences.java
```

Background and cast stay active until the next `[...]` that changes them. Paths are relative to `res/`.
Unknown speakers or actions fail loudly with the file and line number when the sequence starts.
