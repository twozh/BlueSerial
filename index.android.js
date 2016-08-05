/**
 * Sample React Native App
 * https://github.com/facebook/react-native
 * @flow
 */

import React, { Component } from 'react';
import {
  AppRegistry,
  StyleSheet,
  Text,
  View,
  ToastAndroid,
  DeviceEventEmitter
} from 'react-native';

import BlueSerialNativeModule from './BlueSerialNativeModule';
import Button from './components/button.js';

class BlueSerial extends Component {
  constructor(props){
    super(props);
    this.state = {text: ''};
  }

  componentWillMount(){
    DeviceEventEmitter.addListener('deviceFind', function(e) {
      console.log(e);
    });
  }

  async onPressTestButton(){
    try {
      var ret = await BlueSerialNativeModule.testMethod(false);
      ToastAndroid.show(ret, ToastAndroid.SHORT);
      this.setState({text: ret});
      console.log(ret);

    } catch (e){
      console.log(e);
    }
  }

  async onPressStartDiscovery(){
    try {
      var ret = await BlueSerialNativeModule.startDiscovery();
      ToastAndroid.show(ret, ToastAndroid.SHORT);
      console.log(ret);
    } catch(e){
      console.error(e);
    }
    
  }

  render() {
    return (
      <View style={styles.container}>
        <Text style={styles.welcome}>
          Welcome to React Native!
        </Text>
        <Text style={styles.instructions}>
          TestMethod: {this.state.text}
        </Text>
        <Button onPress={this.onPressTestButton.bind(this)}>Test</Button>
        <Button onPress={this.onPressStartDiscovery.bind(this)}>Scan</Button>
      </View>
    );
  }
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    backgroundColor: '#F5FCFF',
  },
  welcome: {
    fontSize: 20,
    textAlign: 'center',
    margin: 10,
  },
  instructions: {
    textAlign: 'center',
    color: '#333333',
    marginBottom: 5,
  },
});

AppRegistry.registerComponent('BlueSerial', () => BlueSerial);
