# Story script: village_awakening
# Edit freely; see res/story/README.md for the format.

speaker aldous = Old Aldous | npc/sage_down_1.png | #C8CDDC

!action start_awakening
!wait 120
[overlay]
aldous: Easy. Easy now. Breathe. There you are.
aldous: You died out there, child. Everyone here has, at least once. And yet you woke up. That's the village for you.
aldous: Welcome. I'm Aldous. I've been waiting for someone like you for longer than you'd believe.
aldous: Come and see me when your legs remember how to work. My house is at the bottom of the village, the old brick one with the broken roof.
!action blink_out
!wait 34
!action remove_awakening_sage
!wait 60
