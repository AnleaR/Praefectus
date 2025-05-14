In this folder, I must include 5 app icons:

1. iconMouseNormal.png | a gray-scale icon for the default state of my app button when it is unselected.
    The file should be smaller than 30KB, and sized at 256X256 pixels with at least 72 PPI.

2. iconMouseOver.png | a colored version of IconMouseNormal.png, for when the app button is selected or mouse-overed.
    Similarly, file should be smaller than 30KB and sized at 256X256 pixels with at least 72 PPI.

3. icon.ico | an icon for overwolf store and desktop launcher.
    It must be 256×256 transparent .png converted into an .ico file in the following resolutions: 16×16, 32×32, 48×48, 256×256.
    In order to create a multi-layer .ico file, you can use a third-party service. Use a tool like convertico (https://convertico.com).
    Make sure that your icon's layer sizes include all of (and only) the above sizes (16×16, 32×32, 48×48, 256×256).
    The launcher icon should weigh less than 150Kb.

4. windowIcon.png | A colored icon for the window task bar icon \ window header. If not defined, `iconMouseOver.png` will be taken.
    The difference between the two icons, is that this taskbar icon should be squared, while the other icons are rounded, to fit the OW deck.
    Similarly, file should be smaller than 30KB and sized 256X256 pixels with at least 72 PPI.

5. trayIcon.png | A dedicated tray icon for my app. The tray icon should look similar to the app's dock icon, to prevent confusion.
    The tray icon is a 32x32 transparent .png converted into an .ico file in the following resolutions: 16x16, 32x32.
    If not defined, this icon will default to the `launcher_icon.ico` image.