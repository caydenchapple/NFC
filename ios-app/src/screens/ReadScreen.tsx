import React, { useCallback, useState } from 'react';
import { ScrollView, StyleSheet, Text } from 'react-native';
import { Card, EmptyState, LabeledRow, PrimaryButton, SectionHeader } from '../components/Primitives';
import { addHistoryEntry, getProfileById } from '../data/storage';
import { readTag } from '../nfc/NfcService';
import { ParsedNdefRecord, TagInfo } from '../nfc/types';
import { findTaskChain } from '../tasks/taskChainNdef';
import { StepResult, actionLabel } from '../tasks/types';
import { runChain } from '../tasks/executor';
import { colors } from '../theme/colors';

export default function ReadScreen() {
  const [loading, setLoading] = useState(false);
  const [tag, setTag] = useState<TagInfo | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [taskResults, setTaskResults] = useState<StepResult[] | null>(null);

  const scan = useCallback(async () => {
    setLoading(true);
    setError(null);
    setTaskResults(null);
    try {
      const info = await readTag();
      setTag(info);
      await addHistoryEntry('READ', `Read ${info.tagType ?? 'tag'} (${info.records.length} records)`, info.idHex);

      const chain = findTaskChain(info.rawRecords);
      if (chain) {
        const results = await runChain(chain, async (id) => (await getProfileById(id))?.taskChain ?? null);
        setTaskResults(results);
        await addHistoryEntry('TASK_RUN', `Ran ${chain.steps.length} attached task step(s)`, info.idHex);
      }
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Could not read the tag.');
    } finally {
      setLoading(false);
    }
  }, []);

  return (
    <ScrollView style={styles.screen} contentContainerStyle={styles.content}>
      <PrimaryButton title={loading ? 'Hold near tag…' : 'Scan a tag'} onPress={scan} loading={loading} />

      {error && (
        <Card style={styles.errorCard}>
          <Text style={styles.errorText}>{error}</Text>
        </Card>
      )}

      {!tag && !error && !loading && (
        <EmptyState title="Ready to scan" subtitle="Tap the button, then hold your iPhone near an NFC tag." />
      )}

      {tag && (
        <>
          <Card>
            <Text style={styles.title}>{tag.tagType ?? 'NFC tag'}</Text>
            <LabeledRow label="ID" value={tag.idHex} />
            <LabeledRow label="Tech" value={tag.techTypes.join(', ') || '—'} />
            {tag.maxSizeBytes != null && <LabeledRow label="Capacity" value={`${tag.maxSizeBytes} bytes`} />}
          </Card>

          {tag.records.length > 0 && (
            <>
              <SectionHeader title="NDEF records" />
              {tag.records.map((record, index) => (
                <RecordCard key={index} record={record} />
              ))}
            </>
          )}

          {taskResults && (
            <Card>
              <Text style={styles.title}>Automation ran</Text>
              {taskResults.map((result, index) => (
                <Text key={index} style={styles.resultLine}>
                  • {actionLabel(result.action)}:{' '}
                  {result.kind === 'skipped' ? `skipped (${result.reason})` : result.outcome.ok ? 'done' : result.outcome.reason}
                </Text>
              ))}
            </Card>
          )}
        </>
      )}
    </ScrollView>
  );
}

function RecordCard({ record }: { record: ParsedNdefRecord }) {
  return (
    <Card style={styles.recordCard}>
      {record.kind === 'text' && (
        <>
          <Text style={styles.recordKind}>Text</Text>
          <Text>{record.text}</Text>
        </>
      )}
      {record.kind === 'uri' && (
        <>
          <Text style={styles.recordKind}>Link</Text>
          <Text>{record.uri}</Text>
        </>
      )}
      {record.kind === 'vcard' && (
        <>
          <Text style={styles.recordKind}>Contact</Text>
          {record.name && <Text>{record.name}</Text>}
          {record.phone && <LabeledRow label="Phone" value={record.phone} />}
          {record.email && <LabeledRow label="Email" value={record.email} />}
        </>
      )}
      {record.kind === 'wifi' && (
        <>
          <Text style={styles.recordKind}>Wi-Fi network</Text>
          <LabeledRow label="SSID" value={record.ssid ?? '?'} />
        </>
      )}
      {record.kind === 'mime' && (
        <>
          <Text style={styles.recordKind}>MIME: {record.mimeType}</Text>
          <Text>{record.byteLength} bytes</Text>
        </>
      )}
      {record.kind === 'external' && (
        <>
          <Text style={styles.recordKind}>External: {record.domain}</Text>
          <Text>{record.type}</Text>
        </>
      )}
      {record.kind === 'unknown' && (
        <>
          <Text style={styles.recordKind}>Unknown record</Text>
          <Text>TNF {record.tnf}, type {record.type}</Text>
        </>
      )}
    </Card>
  );
}

const styles = StyleSheet.create({
  screen: { flex: 1, backgroundColor: colors.background },
  content: { padding: 16, gap: 12 },
  title: { fontSize: 18, fontWeight: '700', color: colors.text },
  errorCard: { backgroundColor: '#FDECEA', borderColor: colors.danger },
  errorText: { color: colors.danger },
  recordCard: { backgroundColor: colors.surfaceAlt },
  recordKind: { fontWeight: '600', color: colors.text },
  resultLine: { color: colors.text },
});
