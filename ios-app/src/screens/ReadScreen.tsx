import React, { useCallback, useState } from 'react';
import { Alert, ScrollView, StyleSheet, Text, View } from 'react-native';
import { Card, EmptyState, LabeledRow, PrimaryButton, SecondaryButton, SectionHeader } from '../components/Primitives';
import { ScreenBackground } from '../components/ScreenBackground';
import { ScanRadar } from '../components/ScanRadar';
import { Icon, IconName } from '../components/Icons';
import { addHistoryEntry, getProfileById, saveProfile } from '../data/storage';
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
  const [saveStatus, setSaveStatus] = useState<string | null>(null);

  const scan = useCallback(async () => {
    setLoading(true);
    setError(null);
    setTaskResults(null);
    setSaveStatus(null);
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

  const saveScannedTag = useCallback(() => {
    if (!tag) return;
    Alert.prompt('Save scanned tag', 'Give this saved tag a name', async (name) => {
      if (!name || !name.trim()) return;
      await saveProfile({ name: name.trim(), records: [], rawRecords: tag.rawRecords, sourceTagId: tag.idHex });
      await addHistoryEntry('SAVE', `Saved scanned tag as "${name.trim()}"`, tag.idHex);
      setSaveStatus(`Saved as "${name.trim()}". Find it under More → Profiles.`);
    });
  }, [tag]);

  return (
    <ScreenBackground>
    <ScrollView style={styles.screen} contentContainerStyle={styles.content}>
      <PrimaryButton title={loading ? 'Hold near tag…' : 'Scan a tag'} onPress={scan} loading={loading} icon="scan" />

      {error && (
        <Card style={styles.errorCard}>
          <Text style={styles.errorText}>{error}</Text>
        </Card>
      )}

      {!tag && !error && !loading && (
        <EmptyState
          title="Ready to scan"
          subtitle="Tap the button, then hold your iPhone near an NFC tag."
          visual={<ScanRadar />}
        />
      )}

      {tag && (
        <>
          <Card accent>
            <Text style={styles.title}>{tag.tagType ?? 'NFC tag'}</Text>
            <LabeledRow label="ID" value={tag.idHex} />
            <LabeledRow label="Tech" value={tag.techTypes.join(', ') || '—'} />
            {tag.maxSizeBytes != null && <LabeledRow label="Capacity" value={`${tag.maxSizeBytes} bytes`} />}
          </Card>

          <SecondaryButton title="Save scanned tag" onPress={saveScannedTag} icon="starOutline" />
          {saveStatus && <Text style={styles.saveStatus}>{saveStatus}</Text>}

          {tag.records.length > 0 && (
            <>
              <SectionHeader title="NDEF records" />
              {tag.records.map((record, index) => (
                <RecordCard key={index} record={record} />
              ))}
            </>
          )}

          {taskResults && (
            <Card accent>
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
    </ScreenBackground>
  );
}

const RECORD_ICONS: Record<ParsedNdefRecord['kind'], IconName> = {
  text: 'document',
  uri: 'link',
  vcard: 'person',
  wifi: 'wifi',
  mime: 'folder',
  external: 'globe',
  unknown: 'alert',
};

function KindLabel({ kind, children }: { kind: ParsedNdefRecord['kind']; children: React.ReactNode }) {
  return (
    <View style={styles.kindRow}>
      <Icon name={RECORD_ICONS[kind]} color={colors.primary} size={14} />
      <Text style={styles.recordKind}>{children}</Text>
    </View>
  );
}

function RecordCard({ record }: { record: ParsedNdefRecord }) {
  return (
    <Card style={styles.recordCard}>
      {record.kind === 'text' && (
        <>
          <KindLabel kind="text">Text</KindLabel>
          <Text style={styles.recordBody}>{record.text}</Text>
        </>
      )}
      {record.kind === 'uri' && (
        <>
          <KindLabel kind="uri">Link</KindLabel>
          <Text style={styles.recordBody}>{record.uri}</Text>
        </>
      )}
      {record.kind === 'vcard' && (
        <>
          <KindLabel kind="vcard">Contact</KindLabel>
          {record.name && <Text style={styles.recordBody}>{record.name}</Text>}
          {record.phone && <LabeledRow label="Phone" value={record.phone} />}
          {record.email && <LabeledRow label="Email" value={record.email} />}
        </>
      )}
      {record.kind === 'wifi' && (
        <>
          <KindLabel kind="wifi">Wi-Fi network</KindLabel>
          <LabeledRow label="SSID" value={record.ssid ?? '?'} />
        </>
      )}
      {record.kind === 'mime' && (
        <>
          <KindLabel kind="mime">MIME: {record.mimeType}</KindLabel>
          <Text style={styles.recordBody}>{record.byteLength} bytes</Text>
        </>
      )}
      {record.kind === 'external' && (
        <>
          <KindLabel kind="external">External: {record.domain}</KindLabel>
          <Text style={styles.recordBody}>{record.type}</Text>
        </>
      )}
      {record.kind === 'unknown' && (
        <>
          <KindLabel kind="unknown">Unknown record</KindLabel>
          <Text style={styles.recordBody}>TNF {record.tnf}, type {record.type}</Text>
        </>
      )}
    </Card>
  );
}

const styles = StyleSheet.create({
  screen: { flex: 1 },
  content: { padding: 16, paddingBottom: 36, gap: 12 },
  title: { fontSize: 18, fontWeight: '700', color: colors.text, letterSpacing: 0.2 },
  errorCard: { backgroundColor: colors.dangerMuted, borderColor: colors.danger },
  errorText: { color: colors.danger, fontWeight: '600' },
  recordCard: { backgroundColor: colors.surfaceAlt },
  kindRow: { flexDirection: 'row', alignItems: 'center', gap: 6 },
  recordKind: { fontWeight: '700', color: colors.primary, fontSize: 12, letterSpacing: 0.6, textTransform: 'uppercase' },
  recordBody: { color: colors.text, fontSize: 14 },
  resultLine: { color: colors.text },
  saveStatus: { color: colors.accentGreenBright, fontWeight: '600', fontSize: 13 },
});
