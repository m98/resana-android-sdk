
<p align="center">
  <img width="200" height="127" src="https://github.com/oddrun/resana-android-sdk-sample/blob/master/app/src/main/res/mipmap-mdpi/resana_logo.png" alt="Resana">
</p>

# Resana android SDK
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
 
    
dependencies {
    implementation( 'io.resana:resana:6.3.1@aar' ) {transitive = true}
}
```
## Documentation
* [User guide](https://github.com/oddrun/resana-android-sdk/blob/master/UserGuide.md): This guide contains examples on how to use Resana in your android project.
* [Change log](https://github.com/oddrun/resana-android-sdk/blob/master/ChangeLog.md): Changes in the recent versions of resana.

