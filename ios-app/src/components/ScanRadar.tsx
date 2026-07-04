import React, { useEffect, useRef } from 'react';
import { Animated, Easing, StyleSheet, View } from 'react-native';
import { colors } from '../theme/colors';
import { Icon } from './Icons';

const RING_COUNT = 3;
const RING_DURATION = 2200;
const STAGGER = RING_DURATION / RING_COUNT;

/** Concentric pulsing rings behind the NFC glyph, echoing a radar/scan sweep
 * so the idle "ready to scan" state feels alive instead of a static icon. */
export function ScanRadar() {
  const values = useRef(Array.from({ length: RING_COUNT }, () => new Animated.Value(0))).current;

  useEffect(() => {
    const loops = values.map((value, i) =>
      Animated.loop(
        Animated.sequence([
          Animated.delay(i * STAGGER),
          Animated.timing(value, {
            toValue: 1,
            duration: RING_DURATION,
            easing: Easing.out(Easing.quad),
            useNativeDriver: true,
          }),
        ]),
      ),
    );
    loops.forEach((loop) => loop.start());
    return () => loops.forEach((loop) => loop.stop());
  }, [values]);

  return (
    <View style={styles.wrap}>
      {values.map((value, i) => {
        const scale = value.interpolate({ inputRange: [0, 1], outputRange: [0.5, 1.9] });
        const opacity = value.interpolate({ inputRange: [0, 0.6, 1], outputRange: [0.55, 0.18, 0] });
        return <Animated.View key={i} style={[styles.ring, { transform: [{ scale }], opacity }]} />;
      })}
      <View style={styles.core}>
        <Icon name="nfc" color={colors.primary} size={30} />
      </View>
    </View>
  );
}

const RING_SIZE = 76;

const styles = StyleSheet.create({
  wrap: {
    width: RING_SIZE,
    height: RING_SIZE,
    alignItems: 'center',
    justifyContent: 'center',
    marginBottom: 6,
  },
  ring: {
    position: 'absolute',
    width: RING_SIZE,
    height: RING_SIZE,
    borderRadius: RING_SIZE / 2,
    borderWidth: 2,
    borderColor: colors.primary,
  },
  core: {
    width: RING_SIZE * 0.62,
    height: RING_SIZE * 0.62,
    borderRadius: (RING_SIZE * 0.62) / 2,
    backgroundColor: colors.primaryMuted,
    borderWidth: 1.5,
    borderColor: colors.borderAccent,
    alignItems: 'center',
    justifyContent: 'center',
  },
});
