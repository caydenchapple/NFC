import React, { useCallback, useEffect, useState } from 'react';
import { ScrollView, StyleSheet, Text, View } from 'react-native';
import { Card, DangerButton, EmptyState } from '../components/Primitives';
import { ScreenBackground } from '../components/ScreenBackground';
import { HistoryEntry } from '../data/types';
import { clearHistory, getHistory } from '../data/storage';
import { colors } from '../theme/colors';

export default function HistoryScreen() {
  const [entries, setEntries] = useState<HistoryEntry[]>([]);

  const reload = useCallback(() => {
    getHistory().then(setEntries);
  }, []);

  useEffect(reload, [reload]);

  const clear = useCallback(async () => {
    await clearHistory();
    reload();
  }, [reload]);

  if (entries.length === 0) {
    return (
      <ScreenBackground>
        <EmptyState title="No history yet" subtitle="Reads, writes, and task runs will show up here." />
      </ScreenBackground>
    );
  }

  return (
    <ScreenBackground>
    <ScrollView style={styles.screen} contentContainerStyle={styles.content}>
      <DangerButton title="Clear history" onPress={clear} icon="trash" />
      {entries.map((entry) => (
        <Card key={entry.id}>
          <View style={styles.row}>
            <Text style={styles.action}>{entry.action}</Text>
            <Text style={styles.time}>{new Date(entry.timestamp).toLocaleString()}</Text>
          </View>
          <Text style={styles.summary}>{entry.summary}</Text>
          {entry.tagId && <Text style={styles.tagId}>ID: {entry.tagId}</Text>}
        </Card>
      ))}
    </ScrollView>
    </ScreenBackground>
  );
}

const styles = StyleSheet.create({
  screen: { flex: 1 },
  content: { padding: 16, paddingBottom: 36, gap: 12 },
  row: { flexDirection: 'row', justifyContent: 'space-between' },
  action: { fontWeight: '700', color: colors.primary, fontSize: 12, letterSpacing: 0.6, textTransform: 'uppercase' },
  time: { color: colors.textFaint, fontSize: 12 },
  summary: { color: colors.text },
  tagId: { color: colors.textMuted, fontSize: 12, marginTop: 2 },
});
