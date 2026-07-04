/**
 * NFC Kit -- iOS
 * @format
 */

import React, { useEffect, useState } from 'react';
import { StatusBar, StyleSheet, Text, View } from 'react-native';
import { GestureHandlerRootView } from 'react-native-gesture-handler';
import { SafeAreaProvider, SafeAreaView } from 'react-native-safe-area-context';
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
        <StatusBar barStyle="light-content" backgroundColor={colors.background} />
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
    <SafeAreaView edges={['top']} style={styles.banner}>
      <View style={styles.bannerDot} />
      <Text style={styles.bannerText}>
        NFC isn't available on this device/simulator. The UI works, but scanning won't.
      </Text>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  flex: { flex: 1 },
  banner: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
    backgroundColor: colors.surfaceAlt,
    borderBottomWidth: 1,
    borderBottomColor: colors.borderStrong,
    paddingVertical: 8,
    paddingHorizontal: 14,
  },
  bannerDot: {
    width: 7,
    height: 7,
    borderRadius: 4,
    backgroundColor: colors.danger,
  },
  bannerText: {
    flex: 1,
    color: colors.textMuted,
    fontSize: 12,
    fontWeight: '600',
  },
});

export default App;
