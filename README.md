# Description - Simple Air Quality Sticker App

This project was created as part of the deliverables for a cross-university course project by TU Eindhoven, OCAD University Toronto, and UTEC Lima University. The motivation for creating the app is to be able to add current Air Quality Health Index (AQHI) information easily on images as stickers - similar to weather information - so it can be easily shared on social media platforms like Instagram and Snapchat. More information about AQHI in Canada can be found [here](http://www.airqualityontario.com/science/aqhi_description.php).

Since the goal of the whole project is to raise awareness on air quality, and since there are also still few open source projects around that involve Canada's AQHI, I decided to share this app project here. As the name implies, at the moment it only provides AQHI stickers for 4 different areas in Toronto: Toronto Downtown, Toronto East, Toronto West, and Toronto North. But it can always be improved to include other locations in Canada, among other features. Feel free to share ideas and suggestions!

# Implementation

I used the default Basic Activity template of Android Studio to build on the app. Upon starting, the app will make a HTTP request to the data server of Environment and Climate Change Canada (ECCC) where it gets current AQHI indices from different measurement stations in Canada. The AQHI indices of Toronto are stored and later on displayed on the image that gets selected to use in the app.

Some libraries and data sources that I used are:
1. Environment and Climate Change Canada HTTP Data Server ([license](http://dd.weather.gc.ca/doc/LICENCE_GENERAL.txt))
2. Jsoup ([license](https://jsoup.org/license))
