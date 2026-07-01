import React, { useCallback, useEffect, useState } from 'react';
import { ScrollView, StyleSheet, Text, View } from 'react-native';
import { Card, EmptyState, SecondaryButton } from '../components/Primitives';
import { Profile } from '../data/types';
import { addHistoryEntry, deleteProfile, getProfiles } from '../data/storage';
import { writeRecords } from '../nfc/NfcService';
import { toNdefRecord } from '../nfc/recordSpec';
import { taskChainToRecord } from '../tasks/taskChainNdef';
import { colors } from '../theme/colors';

export default function ProfilesScreen() {
  const [profiles, setProfiles] = useState<Profile[]>([]);
  const [status, setStatus] = useState<string | null>(null);

  const reload = useCallback(() => {
    getProfiles().then(setProfiles);
  }, []);

  useEffect(reload, [reload]);

  const writeToTag = useCallback(async (profile: Profile) => {
    const records = profile.records.map(toNdefRecord);
    if (profile.taskChain) records.push(taskChainToRecord(profile.taskChain));
    if (records.length === 0) {
      setStatus('This profile has nothing to write.');
      return;
    }
    setStatus('Hold your iPhone near the tag…');
    const result = await writeRecords(records);
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
    return <EmptyState title="No profiles yet" subtitle="Save a record set or task chain from the Write or Tasks tab." />;
  }

  return (
    <ScrollView style={styles.screen} contentContainerStyle={styles.content}>
      {status && <Text style={styles.status}>{status}</Text>}
      {profiles.map((profile) => (
        <Card key={profile.id}>
          <View style={styles.row}>
            <Text style={styles.title}>{profile.name}</Text>
            <SecondaryButton title="Delete" onPress={() => remove(profile)} />
          </View>
          <Text style={styles.subtitle}>
            {profile.records.length} record(s){profile.taskChain ? `, ${profile.taskChain.steps.length} task(s)` : ''}
          </Text>
          <SecondaryButton title="Write to tag" onPress={() => writeToTag(profile)} />
        </Card>
      ))}
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  screen: { flex: 1, backgroundColor: colors.background },
  content: { padding: 16, gap: 12 },
  row: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center' },
  title: { fontSize: 16, fontWeight: '600', color: colors.text },
  subtitle: { color: colors.textMuted, fontSize: 13 },
  status: { color: colors.textMuted },
});
