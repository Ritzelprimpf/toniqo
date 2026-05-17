### Guitar-tuner

A tuner for a guitar with predefined tunings. The user may select a tuning from a list of tunings. The tunings will be categorized.

- 6-String
    - Standard
        - E
        - Es
        - D
        - Des
        - And so on, always a half step down
    - Open
        - Open A
        - Open B
        - Open C
        - Open D
        - Open E
        - Open F
        - Open G
        - DADGAD
    - Dropped
        - Dropped D
        - Dropped Des
        - Dropped C
        - Dropped B
        - Dropped Bes
- 7-String
    - Standard
        - [...]
    - Open
        - [...]
    - Dropped
        - [...]
- 8-String
    - [...]

Please complete the list with typical guitar tunings. Also include modern, very dark metal tunings.  

The tuner module must be able to use the phones microphone to listen to a string played on a guitar.

TODO: This part is guessed and needs improvement
The App needs to translate a tone to its frequency. The frequency is then compared to the frequency of the target tone.
During tuning, the target tune is the tone in the tuning starting from the lowest string (E in standard tuning).
The user should get visual feedback on whether his current tone if flat or sharp compared to the target tone. After the
frequencies match, the next target tone is selected.

This is how I would see the process of tuning:

The user selects a tuning (eG: Dropped C)
The App shows the User the first String to strum (lowest string)
The user strums the string
The App provides Feedback whether the String is flat or sharp
The user adjusts the String
This continues for all Strings of the selected tuning (6 times for 6-string, 7 times for 7-strings, etc)
After the first run, the user can hit the strings at will and the App should be able to guess what string was hit and again, provide feedback on whether it is flat or sharp.
This last step is to fine tune the guitar and should be able to be done in a different order and should be able to target individual strings that are off while others might already be in tune

The selection of the tuning should be intuitive and fast. I would see at least two dropdowns, one that selects the type of guitar and another one that selects the tuning itself.
Having all in one Drop-Down will be too much scrolling inside the dropdown.