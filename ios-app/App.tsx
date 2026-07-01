/**
 * NFC Kit -- iOS
 * @format
 */

import React, { useEffect, useState } from 'react';
import { StatusBar, StyleSheet, Text, View } from 'react-native';
import { GestureHandlerRootView } from 'react-native-gesture-handler';
import { SafeAreaProvider } from 'react-native-safe-area-context';
import RootNavigator from './src/navigation/RootNavigator';
import { ensureStarted } from './src/nfc/NfcService';
import { colors } from './src/theme/colors';

function App() {
  const [nfcSupported, setNfcSupported] = useState<boolean | null>(null);

  useEffect(() => {
    ensureStarted()
      .then(setNfcSupported)
      .catch(() => setNfcSupported(false));
  }, []);

  return (
    <GestureHandlerRootView style={styles.flex}>
      <SafeAreaProvider>
        <StatusBar barStyle="dark-content" />
        {nfcSupported === false && <UnsupportedBanner />}
        <RootNavigator />
      </SafeAreaProvider>
    </GestureHandlerRootView>
  );
}

/** Shown on the iOS Simulator (no NFC radio at all) or an NFC-less iPhone. The rest of
 * the UI still renders so you can see every screen; scanning just won't do anything. */
function UnsupportedBanner() {
  return (
    <View style={styles.banner}>
      <Text style={styles.bannerText}>
        NFC isn't available on this device/simulator. The UI works, but scanning won't.
      </Text>
    </View>
  );
}

const styles = StyleSheet.create({
  flex: { flex: 1 },
  banner: {
    backgroundColor: colors.danger,
    paddingVertical: 6,
    paddingHorizontal: 12,
  },
  bannerText: {
    color: '#fff',
    fontSize: 12,
    textAlign: 'center',
  },
});

export default App;
