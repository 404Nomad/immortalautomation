
when launching the app : 

- Click : Grant overlay & start widget, it oppens your Settings app and you have to allow 
  immortalautomation, then go back and click again, the dot will appear at the center of the screen

- Click : Enable accessibility service, it oppens your Settings app and you have to allow
  in installed apps, immortalautomation , turn on and turn on the shrotcut, 
  a floating buble will appear, go back to immortalautomation app

- Click : Run recorded script -- Run the script

- Click : Save current recording -- Save the script and use currentdate for naming

- Click : View scripts -- let you see each script you created

how it works : 

Feature	                Behaviour

Tap green bubble	    - Expands three buttons (Record • Save • Play).

Record (+) BUTTON	    ‑ Puts the service in recording mode.
                        ‑ A full‑screen transparent overlay starts capturing every tap anywhere on the screen.
                        ‑ Each tap drops a red dot that auto‑vanishes after 750 ms and is stored as a ClickAction.
                        ‑ You can keep tapping to build the sequence.

Save	                - Renames current.json to yyyyMMdd_HHmm.json, clears recording mode, collapses buttons.

Play	                - Immediately plays current.json (the still‑open recording) through the accessibility service.

Collapse	            - Menu auto‑collapses after you press any child button.

No more bubble‑position recording	- We no longer read the bubble’s coordinates. 