
<p align="center">
  <img width="200" height="127" src="https://github.com/oddrun/resana-android-sdk-sample/blob/master/app/src/main/res/mipmap-mdpi/resana_logo.png" alt="Resana">
</p>

# Resana android SDK

[![Codacy Badge](https://api.codacy.com/project/badge/Grade/a8b989790c594609a9811a5991fd0fb9)](https://app.codacy.com/app/ehsansouri23/resana-android-sdk?utm_source=github.com&utm_medium=referral&utm_content=oddrun/resana-android-sdk&utm_campaign=Badge_Grade_Dashboard)

Resana is a mobile ad network. it helps you to improve your bussines.



## Resana Goals
* A simple way of showing splash, video sticky and native ads to host application.
* Native ads are ads that developer of host application decides where to show.
* Resana handles ad click and landing click for native ads itself. <br /> 
* [Read more about resana.](http://resana.io)


## Download
* To use Resana in Android
```ruby
allprojects {
...
    repositories {
      maven { url "https://maven.oddrun.ir/artifactory/resana" }
     }
 }    
 
 ext {
    resanaVersion = 'latest version'
 }
dependencies {
    implementation( 'io.resana:resana:$resanaVersion@aar' ) {transitive = true}
}
```
## Documentation
* [User guide](https://github.com/oddrun/resana-android-sdk/blob/master/UserGuide.md): This guide contains examples on how to use Resana in your android project.
* [Change log](https://github.com/oddrun/resana-android-sdk/blob/master/ChangeLog.md): Changes in the recent versions of resana.

