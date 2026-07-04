import React, { useCallback, useEffect, useState } from 'react';
import { ScrollView, StyleSheet, Text, View } from 'react-native';
import { Card, DangerButton, EmptyState, SecondaryButton } from '../components/Primitives';
import { Icon } from '../components/Icons';
import { ScreenBackground } from '../components/ScreenBackground';
import { Profile } from '../data/types';
import { addHistoryEntry, deleteProfile, getProfiles } from '../data/storage';
import { isScannedCopy, writeProfileToTag } from '../nfc/profileWrite';
import { colors } from '../theme/colors';

export default function ProfilesScreen() {
  const [profiles, setProfiles] = useState<Profile[]>([]);
  const [status, setStatus] = useState<string | null>(null);

  const reload = useCallback(() => {
    getProfiles().then(setProfiles);
  }, []);

  useEffect(reload, [reload]);

  const writeToTag = useCallback(async (profile: Profile) => {
    setStatus('Hold your iPhone near the tag…');
    const result = await writeProfileToTag(profile);
    setStatus(result.ok ? 'Written successfully.' : `Failed: ${result.message}`);
    if (result.ok) await addHistoryEntry('WRITE', `Wrote profile "${profile.name}"`);
  }, []);

  const remove = useCallback(
    async (profile: Profile) => {
      await deleteProfile(profile.id);
      reload();
    },
    [reload],
  );

  if (profiles.length === 0) {
    return (
      <ScreenBackground>
        <EmptyState title="No profiles yet" subtitle="Save a record set or task chain from the Write or Tasks tab." />
      </ScreenBackground>
    );
  }

  return (
    <ScreenBackground>
    <ScrollView style={styles.screen} contentContainerStyle={styles.content}>
      {status && <Text style={styles.status}>{status}</Text>}
      {profiles.map((profile) => {
        const scanned = isScannedCopy(profile);
        return (
          <Card key={profile.id} accent={scanned}>
            <View style={styles.row}>
              <Text style={styles.title}>{profile.name}</Text>
              <DangerButton title="Delete" onPress={() => remove(profile)} icon="trash" />
            </View>
            {scanned ? (
              <View style={styles.scannedRow}>
                <Icon name="scan" color={colors.primary} size={13} />
                <Text style={styles.subtitle}>
                  Scanned tag copy{profile.sourceTagId ? ` · from ${profile.sourceTagId}` : ''}
                </Text>
              </View>
            ) : (
              <Text style={styles.subtitle}>
                {profile.records.length} record(s){profile.taskChain ? `, ${profile.taskChain.steps.length} task(s)` : ''}
              </Text>
            )}
            <SecondaryButton title="Write to tag" onPress={() => writeToTag(profile)} icon="download" />
          </Card>
        );
      })}
    </ScrollView>
    </ScreenBackground>
  );
}

const styles = StyleSheet.create({
  screen: { flex: 1 },
  content: { padding: 16, paddingBottom: 36, gap: 12 },
  row: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center' },
  title: { fontSize: 16, fontWeight: '700', color: colors.text, letterSpacing: 0.2 },
  subtitle: { color: colors.textMuted, fontSize: 13, marginBottom: 4 },
  scannedRow: { flexDirection: 'row', alignItems: 'center', gap: 6, marginBottom: 4 },
  status: { color: colors.accentGreenBright, fontWeight: '600' },
});
