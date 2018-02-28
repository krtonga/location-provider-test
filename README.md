### Description of Project

This app allows someone to both better understand how location services work, by directly seeing the points coming from different Location Providers (and make guesses about where the Fused provider gets theirs). It can be used to make a more informed decision about which Location Provider to use.

This is a screenshot taken when walking a block in NYC, showing that network provided points (the purple markers) are most accurate. 

![screenshot-of-block-to-rc-taken-during-app-development](https://raw.githubusercontent.com/krtonga/location-provider-test/master/block-to-rc.png)

However this is not always the case.

SCREENSHOT FROM TRAIN COMING SOON...

### Developer Notes

The `location` directory in this codebase uses RxJava for location permissions and location updates, and is meant to be modular. In that, I hope that it can easily drop in other projects as needed. 

