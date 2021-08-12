# SimPomo: The Simple Pomodoro Timer

SimPomo is a simple timer app for Android based on the Pomodoro Technique. The timer rotates between 25 minutes for studying and 5 minutes for studying. Every 4 rotations (called pomodoros), the timer starts a longer break of 15 minutes. It is recommended to keep the SimPomo main activity active on your device while studying but not required.

# Features
- A global stats activity keeping track of
	- The overall total time studied
	- The total number of sessions
	- The average time studied per session
	- The longest time studied in a single session
- A foreground notification showing the time remaining when the main activity is not running (but the timer is)
- An option to reset global stats
- The ability to pause the timer within the main activity
- A counter keeping track of total time elapsed in the current active session

# Future Improvements
- Make the study, break, and long break times adjustable by the user
- Keep a database of global stats for all users and increment a leaderboard or rank system

## Preview
![Image of App UI](https://github.com/minizou/pomodoro/blob/main/images/preview_readme.png?raw=true)