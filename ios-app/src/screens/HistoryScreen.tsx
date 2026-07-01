import React, { useCallback, useEffect, useState } from 'react';
import { ScrollView, StyleSheet, Text, View } from 'react-native';
import { Card, EmptyState, SecondaryButton } from '../components/Primitives';
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
    return <EmptyState title="No history yet" subtitle="Reads, writes, and task runs will show up here." />;
  }

  return (
    <ScrollView style={styles.screen} contentContainerStyle={styles.content}>
      <SecondaryButton title="Clear history" onPress={clear} />
      {entries.map((entry) => (
        <Card key={entry.id}>
          <View style={styles.row}>
            <Text style={styles.action}>{entry.action}</Text>
            <Text style={styles.time}>{new Date(entry.timestamp).toLocaleString()}</Text>
          </View>
          <Text>{entry.summary}</Text>
          {entry.tagId && <Text style={styles.tagId}>ID: {entry.tagId}</Text>}
        </Card>
      ))}
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  screen: { flex: 1, backgroundColor: colors.background },
  content: { padding: 16, gap: 12 },
  row: { flexDirection: 'row', justifyContent: 'space-between' },
  action: { fontWeight: '700', color: colors.text },
  time: { color: colors.textMuted, fontSize: 12 },
  tagId: { color: colors.textMuted, fontSize: 12 },
});
