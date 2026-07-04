import React, { PropsWithChildren, useRef } from 'react';
import {
  ActivityIndicator,
  Animated,
  Pressable,
  StyleProp,
  StyleSheet,
  Text,
  View,
  ViewStyle,
} from 'react-native';
import LinearGradient from 'react-native-linear-gradient';
import { trigger } from 'react-native-haptic-feedback';
import { colors } from '../theme/colors';
import { Icon, IconName } from './Icons';

type HapticKind = 'impactLight' | 'impactMedium' | 'impactHeavy';

/** Shared press-scale + haptic logic behind every button. Kept as a hook
 * (rather than baking the animated node into one wrapper component) so
 * callers can choose exactly where the transform lands in their view tree —
 * nesting an extra flex:1 View around a LinearGradient tends to confuse
 * Yoga's sizing in auto-height scroll contexts. */
function usePressableScale(onPress: () => void, disabled: boolean | undefined, haptic: HapticKind) {
  const scale = useRef(new Animated.Value(1)).current;

  const pressIn = () => {
    Animated.spring(scale, { toValue: 0.96, useNativeDriver: true, speed: 60, bounciness: 4 }).start();
  };
  const pressOut = () => {
    Animated.spring(scale, { toValue: 1, useNativeDriver: true, speed: 20, bounciness: 9 }).start();
  };
  const handlePress = () => {
    if (!disabled) {
      trigger(haptic, { enableVibrateFallback: true, ignoreAndroidSystemSettings: false });
      onPress();
    }
  };

  return { scale, pressIn, pressOut, handlePress };
}

/** Shared tactile wrapper for the flat (non-gradient) buttons: scales down on
 * press with a spring-back release, plus a light haptic tick. */
function PressableScale({
  onPress,
  disabled,
  style,
  haptic = 'impactLight',
  children,
}: PropsWithChildren<{
  onPress: () => void;
  disabled?: boolean;
  style?: StyleProp<ViewStyle>;
  haptic?: HapticKind;
}>) {
  const { scale, pressIn, pressOut, handlePress } = usePressableScale(onPress, disabled, haptic);

  return (
    <Pressable onPress={handlePress} onPressIn={pressIn} onPressOut={pressOut} disabled={disabled} style={style}>
      <Animated.View style={[styles.pressableFill, { transform: [{ scale }] }]}>{children}</Animated.View>
    </Pressable>
  );
}

export function Card({
  children,
  style,
  accent = false,
}: PropsWithChildren<{ style?: ViewStyle; accent?: boolean }>) {
  return (
    <View style={[styles.card, accent && styles.cardAccent, style]}>
      {accent && (
        <LinearGradient
          colors={[colors.primary, colors.accentGreenBright]}
          start={{ x: 0, y: 0 }}
          end={{ x: 1, y: 0 }}
          style={styles.cardAccentBar}
        />
      )}
      <View style={styles.cardBody}>{children}</View>
    </View>
  );
}

export function SectionHeader({ title }: { title: string }) {
  return (
    <View style={styles.sectionHeaderRow}>
      <View style={styles.sectionHeaderBar} />
      <Text style={styles.sectionHeader}>{title.toUpperCase()}</Text>
    </View>
  );
}

export function LabeledRow({ label, value }: { label: string; value: string }) {
  return (
    <View style={styles.row}>
      <Text style={styles.label}>{label}</Text>
      <Text style={styles.value}>{value}</Text>
    </View>
  );
}

export function EmptyState({
  title,
  subtitle,
  visual,
}: {
  title: string;
  subtitle: string;
  visual?: React.ReactNode;
}) {
  return (
    <View style={styles.emptyState}>
      {visual ?? (
        <View style={styles.emptyRing}>
          <View style={styles.emptyDot} />
        </View>
      )}
      <Text style={styles.emptyTitle}>{title}</Text>
      <Text style={styles.emptySubtitle}>{subtitle}</Text>
    </View>
  );
}

/** Balanced icon-left / centered-text row used by every pill button, so the
 * label sits dead-center regardless of whether a leading icon is present. */
function ButtonContent({
  title,
  icon,
  iconColor,
  textStyle,
}: {
  title: string;
  icon?: IconName;
  iconColor: string;
  textStyle: object;
}) {
  return (
    <View style={styles.buttonContent}>
      <View style={styles.buttonSlot}>{icon && <Icon name={icon} color={iconColor} size={19} />}</View>
      <Text style={textStyle} numberOfLines={1} adjustsFontSizeToFit minimumFontScale={0.8}>
        {title}
      </Text>
      <View style={styles.buttonSlot} />
    </View>
  );
}

export function PrimaryButton({
  title,
  onPress,
  disabled,
  loading,
  icon,
}: {
  title: string;
  onPress: () => void;
  disabled?: boolean;
  loading?: boolean;
  icon?: IconName;
}) {
  const isDisabled = disabled || loading;
  const { scale, pressIn, pressOut, handlePress } = usePressableScale(onPress, isDisabled, 'impactLight');

  return (
    <Pressable
      onPress={handlePress}
      onPressIn={pressIn}
      onPressOut={pressOut}
      disabled={isDisabled}
      style={[styles.primaryButton, isDisabled && styles.buttonDisabled]}
    >
      <LinearGradient
        colors={[colors.primaryBright, colors.primary, colors.primaryDark]}
        start={{ x: 0, y: 0 }}
        end={{ x: 1, y: 1 }}
        style={StyleSheet.absoluteFill}
      />
      <Animated.View style={[styles.pressableFill, { transform: [{ scale }] }]}>
        {loading ? (
          <ActivityIndicator color={colors.onPrimary} />
        ) : (
          <ButtonContent title={title} icon={icon} iconColor={colors.onPrimary} textStyle={styles.primaryButtonText} />
        )}
      </Animated.View>
    </Pressable>
  );
}

export function SecondaryButton({
  title,
  onPress,
  disabled,
  icon,
}: {
  title: string;
  onPress: () => void;
  disabled?: boolean;
  icon?: IconName;
}) {
  return (
    <PressableScale style={[styles.secondaryButton, disabled && styles.buttonDisabled]} onPress={onPress} disabled={disabled}>
      <ButtonContent
        title={title}
        icon={icon}
        iconColor={colors.accentGreenBright}
        textStyle={styles.secondaryButtonText}
      />
    </PressableScale>
  );
}

export function DangerButton({
  title,
  onPress,
  disabled,
  icon,
}: {
  title: string;
  onPress: () => void;
  disabled?: boolean;
  icon?: IconName;
}) {
  return (
    <PressableScale
      style={[styles.dangerButton, disabled && styles.buttonDisabled]}
      onPress={onPress}
      disabled={disabled}
      haptic="impactMedium"
    >
      <ButtonContent title={title} icon={icon} iconColor={colors.danger} textStyle={styles.dangerButtonText} />
    </PressableScale>
  );
}

const styles = StyleSheet.create({
  card: {
    backgroundColor: colors.surface,
    borderRadius: 18,
    borderWidth: 1,
    borderColor: colors.border,
    overflow: 'hidden',
    shadowColor: '#000000',
    shadowOffset: { width: 0, height: 6 },
    shadowOpacity: 0.4,
    shadowRadius: 10,
    elevation: 3,
  },
  cardAccent: {
    borderColor: colors.borderAccent,
  },
  cardAccentBar: {
    height: 3,
    width: '100%',
    backgroundColor: colors.primary,
  },
  cardBody: {
    padding: 18,
    gap: 10,
  },
  sectionHeaderRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
    marginTop: 10,
    marginBottom: 2,
  },
  sectionHeaderBar: {
    width: 3,
    height: 14,
    borderRadius: 2,
    backgroundColor: colors.primary,
  },
  sectionHeader: {
    fontSize: 13,
    fontWeight: '700',
    color: colors.textMuted,
    letterSpacing: 1.4,
  },
  row: {
    flexDirection: 'row',
    justifyContent: 'space-between',
  },
  label: {
    color: colors.textMuted,
    fontSize: 14,
  },
  value: {
    color: colors.text,
    fontSize: 14,
    fontWeight: '600',
    flexShrink: 1,
    textAlign: 'right',
  },
  emptyState: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
    padding: 32,
    gap: 10,
  },
  emptyRing: {
    width: 72,
    height: 72,
    borderRadius: 36,
    borderWidth: 2,
    borderColor: colors.accentGreen,
    alignItems: 'center',
    justifyContent: 'center',
    marginBottom: 6,
    backgroundColor: colors.accentGreenMuted,
  },
  emptyDot: {
    width: 14,
    height: 14,
    borderRadius: 7,
    backgroundColor: colors.primary,
  },
  emptyTitle: {
    fontSize: 20,
    fontWeight: '700',
    color: colors.text,
  },
  emptySubtitle: {
    fontSize: 14,
    color: colors.textMuted,
    textAlign: 'center',
    lineHeight: 20,
  },
  buttonContent: {
    flexDirection: 'row',
    alignItems: 'center',
    width: '100%',
  },
  buttonSlot: {
    width: 22,
    alignItems: 'center',
    justifyContent: 'center',
  },
  pressableFill: {
    flex: 1,
  },
  primaryButton: {
    flex: 1,
    borderRadius: 28,
    paddingVertical: 16,
    paddingHorizontal: 14,
    overflow: 'hidden',
    shadowColor: colors.primary,
    shadowOffset: { width: 0, height: 5 },
    shadowOpacity: 0.45,
    shadowRadius: 14,
    elevation: 6,
  },
  primaryButtonText: {
    color: colors.onPrimary,
    fontWeight: '700',
    fontSize: 15,
    letterSpacing: 0.3,
    flex: 1,
    textAlign: 'center',
  },
  secondaryButton: {
    borderRadius: 28,
    paddingVertical: 16,
    paddingHorizontal: 14,
    flex: 1,
    borderWidth: 1.5,
    borderColor: colors.accentGreen,
    backgroundColor: colors.accentGreenMuted,
  },
  secondaryButtonText: {
    color: colors.accentGreenBright,
    fontWeight: '700',
    fontSize: 15,
    letterSpacing: 0.3,
    flex: 1,
    textAlign: 'center',
  },
  dangerButton: {
    borderRadius: 28,
    paddingVertical: 16,
    paddingHorizontal: 14,
    flex: 1,
    borderWidth: 1.5,
    borderColor: colors.danger,
    backgroundColor: colors.dangerMuted,
  },
  dangerButtonText: {
    color: colors.danger,
    fontWeight: '700',
    fontSize: 15,
    letterSpacing: 0.3,
    flex: 1,
    textAlign: 'center',
  },
  buttonDisabled: {
    opacity: 0.35,
    shadowOpacity: 0,
  },
});
