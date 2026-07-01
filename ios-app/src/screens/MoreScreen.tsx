import React, { useCallback, useState } from 'react';
import { Alert, Modal, ScrollView, Share, StyleSheet, Text, TextInput, View } from 'react-native';
import { NdefRecord } from 'react-native-nfc-manager';
import { Card, PrimaryButton, SecondaryButton, SectionHeader } from '../components/Primitives';
import { addHistoryEntry, exportProfilesJson, importProfilesJson } from '../data/storage';
import { cloneCapture, cloneWrite, eraseTag, lockTag } from '../nfc/NfcService';
import { colors } from '../theme/colors';

export default function MoreScreen({
  onOpenProfiles,
  onOpenHistory,
}: {
  onOpenProfiles: () => void;
  onOpenHistory: () => void;
}) {
  const [captured, setCaptured] = useState<NdefRecord[] | null>(null);
  const [status, setStatus] = useState<string | null>(null);
  const [importOpen, setImportOpen] = useState(false);

  const runCloneCapture = useCallback(async () => {
    setStatus('Scan the source tag…');
    const result = await cloneCapture();
    if ('error' in result) {
      setStatus(`Failed: ${result.error}`);
      return;
    }
    setCaptured(result.records);
    setStatus('Source captured. Now scan the target tag.');
  }, []);

  const runCloneWrite = useCallback(async () => {
    if (!captured) return;
    setStatus('Scan the target tag…');
    const result = await cloneWrite(captured);
    setStatus(result.ok ? 'Tag cloned successfully.' : `Failed: ${result.message}`);
    if (result.ok) {
      setCaptured(null);
      await addHistoryEntry('CLONE', 'Tag cloned successfully');
    }
  }, [captured]);

  const runErase = useCallback(async () => {
    setStatus('Hold your iPhone near the tag to erase…');
    const result = await eraseTag();
    setStatus(result.ok ? 'Tag erased.' : `Failed: ${result.message}`);
    if (result.ok) await addHistoryEntry('ERASE', 'Tag erased');
  }, []);

  const confirmLock = useCallback(() => {
    Alert.alert('Lock tag permanently?', "This can't be undone. The tag will never be writable again.", [
      { text: 'Cancel', style: 'cancel' },
      {
        text: 'Lock',
        style: 'destructive',
        onPress: async () => {
          setStatus('Hold your iPhone near the tag to lock it…');
          const result = await lockTag();
          setStatus(result.ok ? 'Tag locked read-only.' : `Failed: ${result.message}`);
          if (result.ok) await addHistoryEntry('LOCK', 'Tag locked read-only');
        },
      },
    ]);
  }, []);

  const exportProfiles = useCallback(async () => {
    const json = await exportProfilesJson();
    await Share.share({ message: json, title: 'NFC Kit profiles' });
  }, []);

  return (
    <ScrollView style={styles.screen} contentContainerStyle={styles.content}>
      <SectionHeader title="Tag maintenance" />

      <Card>
        <Text style={styles.cardTitle}>Copy / clone tag</Text>
        <Text style={styles.cardSubtitle}>Scan a source tag, then scan a target tag to duplicate it.</Text>
        <View style={styles.buttonRow}>
          <SecondaryButton title="1. Scan source" onPress={runCloneCapture} />
          <PrimaryButton title="2. Scan target" onPress={runCloneWrite} disabled={!captured} />
        </View>
      </Card>

      <Card>
        <Text style={styles.cardTitle}>Erase tag</Text>
        <Text style={styles.cardSubtitle}>Clears the NDEF content.</Text>
        <SecondaryButton title="Erase" onPress={runErase} />
      </Card>

      <Card>
        <Text style={styles.cardTitle}>Lock tag</Text>
        <Text style={styles.cardSubtitle}>Makes it permanently read-only. Not every tag supports this, and it cannot be undone.</Text>
        <SecondaryButton title="Lock" onPress={confirmLock} />
      </Card>

      {status && (
        <Card style={styles.statusCard}>
          <Text>{status}</Text>
        </Card>
      )}

      <SectionHeader title="Library" />
      <Card>
        <SecondaryButton title="Profiles" onPress={onOpenProfiles} />
      </Card>
      <Card>
        <SecondaryButton title="History" onPress={onOpenHistory} />
      </Card>

      <SectionHeader title="Backup" />
      <View style={styles.buttonRow}>
        <SecondaryButton title="Export profiles" onPress={exportProfiles} />
        <SecondaryButton title="Import profiles" onPress={() => setImportOpen(true)} />
      </View>

      <ImportModal visible={importOpen} onClose={() => setImportOpen(false)} />
    </ScrollView>
  );
}

function ImportModal({ visible, onClose }: { visible: boolean; onClose: () => void }) {
  const [text, setText] = useState('');
  const [error, setError] = useState<string | null>(null);

  const doImport = async () => {
    try {
      const count = await importProfilesJson(text);
      setText('');
      setError(null);
      onClose();
      Alert.alert('Imported', `Imported ${count} profile(s).`);
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Invalid backup file.');
    }
  };

  return (
    <Modal visible={visible} animationType="slide" onRequestClose={onClose} presentationStyle="pageSheet">
      <ScrollView style={styles.screen} contentContainerStyle={styles.content}>
        <SectionHeader title="Import profiles" />
        <Text style={styles.cardSubtitle}>Paste a backup JSON exported from NFC Kit.</Text>
        <TextInput
          style={styles.multiline}
          value={text}
          onChangeText={setText}
          multiline
          numberOfLines={10}
          placeholder="{ &quot;formatVersion&quot;: 1, ... }"
          placeholderTextColor={colors.textMuted}
        />
        {error && <Text style={styles.error}>{error}</Text>}
        <View style={styles.buttonRow}>
          <SecondaryButton title="Cancel" onPress={onClose} />
          <PrimaryButton title="Import" onPress={doImport} disabled={text.trim().length === 0} />
        </View>
      </ScrollView>
    </Modal>
  );
}

const styles = StyleSheet.create({
  screen: { flex: 1, backgroundColor: colors.background },
  content: { padding: 16, gap: 12 },
  cardTitle: { fontSize: 16, fontWeight: '600', color: colors.text },
  cardSubtitle: { color: colors.textMuted, fontSize: 13 },
  buttonRow: { flexDirection: 'row', gap: 8, marginTop: 8 },
  statusCard: { backgroundColor: colors.surfaceAlt },
  multiline: {
    borderWidth: 1,
    borderColor: colors.border,
    borderRadius: 8,
    padding: 12,
    minHeight: 200,
    textAlignVertical: 'top',
    color: colors.text,
  },
  error: { color: colors.danger },
});
