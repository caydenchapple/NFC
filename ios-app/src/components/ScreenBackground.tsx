import React, { PropsWithChildren } from 'react';
import { StyleSheet, View, ViewStyle } from 'react-native';
import LinearGradient from 'react-native-linear-gradient';
import { colors } from '../theme/colors';

function GlowOrb({ color, size, style }: { color: string; size: number; style?: ViewStyle }) {
  return (
    <View
      pointerEvents="none"
      style={[{ position: 'absolute', width: size, height: size, borderRadius: size / 2, overflow: 'hidden' }, style]}
    >
      <LinearGradient
        colors={[color, 'transparent']}
        start={{ x: 0.5, y: 0.15 }}
        end={{ x: 0.5, y: 1 }}
        style={StyleSheet.absoluteFill}
      />
    </View>
  );
}

/** Wraps every screen in a shared black canvas with two soft ambient glows
 * (orange + green, matching the brand palette) instead of a flat, lifeless
 * black rectangle. Purely decorative, sits behind all screen content. */
export function ScreenBackground({ children }: PropsWithChildren<unknown>) {
  return (
    <View style={styles.root}>
      <GlowOrb color="rgba(245, 161, 0, 0.16)" size={300} style={styles.glowTopRight} />
      <GlowOrb color="rgba(52, 199, 126, 0.14)" size={260} style={styles.glowBottomLeft} />
      {children}
    </View>
  );
}

const styles = StyleSheet.create({
  root: {
    flex: 1,
    backgroundColor: colors.background,
    overflow: 'hidden',
  },
  glowTopRight: {
    top: -120,
    right: -90,
  },
  glowBottomLeft: {
    bottom: -100,
    left: -110,
  },
});
