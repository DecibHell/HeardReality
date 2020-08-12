This application lets you play soundwalks in 3D audio with a head-tracker.
It's only a prototype, coded as support for a thesis about "Locative and Spatial Audio" (Pierrick Chauvet 2020, MMT, Trinity College Dublin).

It uses :
- A Nordic Thingy:52 head-tracker
- Google Resonance Audio
- Google Services (Maps, Firebase)

Demonstration Video : "https://www.youtube.com/watch?v=EvFLKGN2q5c&feature=youtu.be"

Two files are missing to compile the project because they contain private credentials :
- google-services.json
- src/debug/res/values/google_maps_api.xml
If you want to compile the project, you will need to create your own version of the Firebase Database and get a Google API key for Firebase and Google Maps.
You can contact me at pierrick.chauvet@gmail.com for details.
