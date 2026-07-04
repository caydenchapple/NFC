import React, { useCallback, useEffect, useState } from 'react';
import { Alert, Modal, ScrollView, StyleSheet, Text, TextInput, TouchableOpacity, View } from 'react-native';
import { trigger } from 'react-native-haptic-feedback';
import { Card, EmptyState, PrimaryButton, SecondaryButton, SectionHeader } from '../components/Primitives';
import { ScreenBackground } from '../components/ScreenBackground';
import { Icon, IconName } from '../components/Icons';
import { Profile } from '../data/types';
import { addHistoryEntry, getProfiles, saveProfile } from '../data/storage';
import { writeRecords } from '../nfc/NfcService';
import { isScannedCopy, writeProfileToTag } from '../nfc/profileWrite';
import { RecordSpec, SocialNetwork, WifiAuthType, recordLabel, toNdefRecord } from '../nfc/recordSpec';
import { colors } from '../theme/colors';

type RecordKind = RecordSpec['kind'];

const KIND_LABELS: Record<RecordKind, string> = {
  text: 'Plain text',
  url: 'URL',
  phone: 'Phone call',
  sms: 'Text message',
  email: 'Email',
  geo: 'Geo location',
  social: 'Social profile',
  wifi: 'Wi-Fi config',
  vcard: 'Contact card',
  video: 'Video link',
  appLink: 'App / universal link',
};

const KIND_ICONS: Record<RecordKind, IconName> = {
  text: 'document',
  url: 'link',
  phone: 'call',
  sms: 'chat',
  email: 'mail',
  geo: 'location',
  social: 'globe',
  wifi: 'wifi',
  vcard: 'person',
  video: 'video',
  appLink: 'shareUp',
};

const SOCIAL_NETWORKS: SocialNetwork[] = ['INSTAGRAM', 'X_TWITTER', 'FACEBOOK', 'LINKEDIN', 'TIKTOK', 'YOUTUBE', 'GITHUB', 'WHATSAPP'];
const WIFI_AUTH_TYPES: WifiAuthType[] = ['OPEN', 'WPA_PERSONAL', 'WPA2_PERSONAL'];

export default function WriteScreen() {
  const [records, setRecords] = useState<RecordSpec[]>([]);
  const [status, setStatus] = useState<'idle' | 'writing' | 'success' | { error: string }>('idle');
  const [savedPickerOpen, setSavedPickerOpen] = useState(false);

  const write = useCallback(async () => {
    if (records.length === 0) return;
    setStatus('writing');
    const ndefRecords = records.map(toNdefRecord);
    const result = await writeRecords(ndefRecords);
    if (result.ok) {
      setStatus('success');
      await addHistoryEntry('WRITE', `Wrote ${records.length} record(s)`);
    } else {
      setStatus({ error: result.message });
    }
  }, [records]);

  const saveAsProfile = useCallback(() => {
    if (records.length === 0) return;
    Alert.prompt('Save as profile', 'Profile name', (name) => {
      if (!name) return;
      saveProfile({ name, records });
    });
  }, [records]);

  return (
    <ScreenBackground>
    <ScrollView style={styles.screen} contentContainerStyle={styles.content}>
      <SecondaryButton
        title="Write a saved tag"
        onPress={() => setSavedPickerOpen(true)}
        icon="folder"
      />
      <Text style={styles.hint}>
        Scanned a tag on the Read tab and saved it? Write that exact copy to another tag from here.
      </Text>

      <SectionHeader title="Build a new record" />
      <AddRecordForm onAdd={(spec) => setRecords((prev) => [...prev, spec])} />

      {records.length > 0 && (
        <>
          <SectionHeader title="Records to write" />
          {records.map((spec, index) => (
            <Card key={index} style={styles.recordRow}>
              <View style={styles.recordRowLeft}>
                <Icon name={KIND_ICONS[spec.kind]} color={colors.primary} size={16} />
                <Text style={styles.recordText}>{recordLabel(spec)}</Text>
              </View>
              <TouchableOpacity
                style={styles.removeButton}
                onPress={() => {
                  trigger('impactLight');
                  setRecords((prev) => prev.filter((_, i) => i !== index));
                }}
                hitSlop={{ top: 8, bottom: 8, left: 8, right: 8 }}
              >
                <Icon name="close" color={colors.danger} size={15} />
              </TouchableOpacity>
            </Card>
          ))}
        </>
      )}

      {status === 'writing' && <Text style={styles.status}>Hold your iPhone near the tag…</Text>}
      {status === 'success' && <Text style={styles.statusSuccess}>Written successfully.</Text>}
      {typeof status === 'object' && <Text style={styles.statusError}>Failed: {status.error}</Text>}

      <View style={styles.buttonRow}>
        <SecondaryButton title="Save as profile" onPress={saveAsProfile} disabled={records.length === 0} icon="starOutline" />
        <PrimaryButton title="Write to tag" onPress={write} disabled={records.length === 0} icon="download" />
      </View>

      <SavedTagPickerModal visible={savedPickerOpen} onClose={() => setSavedPickerOpen(false)} />
    </ScrollView>
    </ScreenBackground>
  );
}

function SavedTagPickerModal({ visible, onClose }: { visible: boolean; onClose: () => void }) {
  const [profiles, setProfiles] = useState<Profile[]>([]);
  const [writingId, setWritingId] = useState<string | null>(null);
  const [rowStatus, setRowStatus] = useState<{
    id: string;
    kind: 'progress' | 'success' | 'error';
    message: string;
  } | null>(null);

  useEffect(() => {
    if (visible) getProfiles().then(setProfiles);
  }, [visible]);

  const writeSaved = async (profile: Profile) => {
    setWritingId(profile.id);
    setRowStatus({ id: profile.id, kind: 'progress', message: 'Hold your iPhone near the tag…' });
    const result = await writeProfileToTag(profile);
    setWritingId(null);
    if (result.ok) {
      setRowStatus({ id: profile.id, kind: 'success', message: 'Written successfully.' });
      await addHistoryEntry('WRITE', `Wrote saved tag "${profile.name}"`);
    } else {
      setRowStatus({ id: profile.id, kind: 'error', message: `Failed: ${result.message}` });
    }
  };

  return (
    <Modal visible={visible} animationType="slide" onRequestClose={onClose} presentationStyle="pageSheet">
      <ScreenBackground>
      <ScrollView style={styles.modalScreen} contentContainerStyle={styles.content}>
        <SectionHeader title="Write a saved tag" />

        {profiles.length === 0 ? (
          <EmptyState
            title="No saved tags yet"
            subtitle="Scan a tag on the Read tab and tap “Save scanned tag”, or save a record set from here first."
          />
        ) : (
          profiles.map((profile) => {
            const scanned = isScannedCopy(profile);
            return (
              <Card key={profile.id} accent={scanned} style={styles.savedRow}>
                <View style={styles.savedRowHeader}>
                  <Icon name={scanned ? 'scan' : 'document'} color={colors.primary} size={15} />
                  <Text style={styles.savedTitle}>{profile.name}</Text>
                </View>
                <Text style={styles.subtitle}>
                  {scanned
                    ? `Scanned tag copy${profile.sourceTagId ? ` · from ${profile.sourceTagId}` : ''}`
                    : `${profile.records.length} record(s)${profile.taskChain ? `, ${profile.taskChain.steps.length} task(s)` : ''}`}
                </Text>
                {rowStatus?.id === profile.id && (
                  <Text
                    style={
                      rowStatus.kind === 'success'
                        ? styles.statusSuccess
                        : rowStatus.kind === 'error'
                          ? styles.statusError
                          : styles.status
                    }
                  >
                    {rowStatus.message}
                  </Text>
                )}
                <PrimaryButton
                  title="Write to tag"
                  onPress={() => writeSaved(profile)}
                  loading={writingId === profile.id}
                  icon="download"
                />
              </Card>
            );
          })
        )}

        <SecondaryButton title="Close" onPress={onClose} icon="close" />
      </ScrollView>
      </ScreenBackground>
    </Modal>
  );
}

function AddRecordForm({ onAdd }: { onAdd: (spec: RecordSpec) => void }) {
  const [kind, setKind] = useState<RecordKind>('text');
  const [f1, setF1] = useState('');
  const [f2, setF2] = useState('');
  const [f3, setF3] = useState('');
  const [social, setSocial] = useState<SocialNetwork>('INSTAGRAM');
  const [wifiAuth, setWifiAuth] = useState<WifiAuthType>('WPA2_PERSONAL');

  const reset = () => {
    setF1('');
    setF2('');
    setF3('');
  };

  const add = () => {
    const spec = buildSpec(kind, f1, f2, f3, social, wifiAuth);
    if (!spec) return;
    onAdd(spec);
    reset();
  };

  return (
    <Card>
      <Text style={styles.formTitle}>Add a record</Text>
      <ChipRow
        options={Object.keys(KIND_LABELS) as RecordKind[]}
        selected={kind}
        labelFor={(k) => KIND_LABELS[k]}
        iconFor={(k) => KIND_ICONS[k]}
        onSelect={(k) => {
          setKind(k);
          reset();
        }}
      />

      {kind === 'text' && <Field placeholder="Text" value={f1} onChangeText={setF1} />}
      {kind === 'url' && <Field placeholder="URL" value={f1} onChangeText={setF1} keyboardType="url" />}
      {kind === 'phone' && <Field placeholder="Phone number" value={f1} onChangeText={setF1} keyboardType="phone-pad" />}
      {kind === 'sms' && (
        <>
          <Field placeholder="Phone number" value={f1} onChangeText={setF1} keyboardType="phone-pad" />
          <Field placeholder="Message (optional)" value={f2} onChangeText={setF2} />
        </>
      )}
      {kind === 'email' && (
        <>
          <Field placeholder="Address" value={f1} onChangeText={setF1} keyboardType="email-address" />
          <Field placeholder="Subject (optional)" value={f2} onChangeText={setF2} />
          <Field placeholder="Body (optional)" value={f3} onChangeText={setF3} />
        </>
      )}
      {kind === 'geo' && (
        <>
          <Field placeholder="Latitude" value={f1} onChangeText={setF1} keyboardType="numbers-and-punctuation" />
          <Field placeholder="Longitude" value={f2} onChangeText={setF2} keyboardType="numbers-and-punctuation" />
        </>
      )}
      {kind === 'social' && (
        <>
          <ChipRow options={SOCIAL_NETWORKS} selected={social} labelFor={(n) => n} onSelect={setSocial} />
          <Field placeholder="Username" value={f1} onChangeText={setF1} />
        </>
      )}
      {kind === 'wifi' && (
        <>
          <Field placeholder="SSID" value={f1} onChangeText={setF1} />
          <Field placeholder="Password" value={f2} onChangeText={setF2} secureTextEntry />
          <ChipRow options={WIFI_AUTH_TYPES} selected={wifiAuth} labelFor={(a) => a} onSelect={setWifiAuth} />
        </>
      )}
      {kind === 'vcard' && (
        <>
          <Field placeholder="Name" value={f1} onChangeText={setF1} />
          <Field placeholder="Phone (optional)" value={f2} onChangeText={setF2} keyboardType="phone-pad" />
          <Field placeholder="Email (optional)" value={f3} onChangeText={setF3} keyboardType="email-address" />
        </>
      )}
      {kind === 'video' && <Field placeholder="Video URL" value={f1} onChangeText={setF1} keyboardType="url" />}
      {kind === 'appLink' && <Field placeholder="App URL scheme or universal link" value={f1} onChangeText={setF1} />}

      <PrimaryButton title="Add record" onPress={add} icon="plus" />
    </Card>
  );
}

function ChipRow<T extends string>({
  options,
  selected,
  labelFor,
  iconFor,
  onSelect,
}: {
  options: T[];
  selected: T;
  labelFor: (option: T) => string;
  iconFor?: (option: T) => IconName;
  onSelect: (option: T) => void;
}) {
  return (
    <ScrollView horizontal showsHorizontalScrollIndicator={false} style={styles.chipRow}>
      {options.map((option) => {
        const active = option === selected;
        return (
          <TouchableOpacity
            key={option}
            style={[styles.chip, active && styles.chipSelected]}
            onPress={() => {
              trigger('selection');
              onSelect(option);
            }}
            activeOpacity={0.75}
          >
            {iconFor && (
              <Icon name={iconFor(option)} color={active ? colors.onPrimary : colors.textMuted} size={14} />
            )}
            <Text style={[styles.chipText, active && styles.chipTextSelected]}>{labelFor(option)}</Text>
          </TouchableOpacity>
        );
      })}
    </ScrollView>
  );
}

function Field(props: React.ComponentProps<typeof TextInput>) {
  return <TextInput {...props} style={styles.input} placeholderTextColor={colors.textMuted} />;
}

function buildSpec(
  kind: RecordKind,
  f1: string,
  f2: string,
  f3: string,
  social: SocialNetwork,
  wifiAuth: WifiAuthType,
): RecordSpec | null {
  switch (kind) {
    case 'text':
      return f1.trim() ? { kind, text: f1 } : null;
    case 'url':
      return f1.trim() ? { kind, url: f1 } : null;
    case 'phone':
      return f1.trim() ? { kind, number: f1 } : null;
    case 'sms':
      return f1.trim() ? { kind, number: f1, message: f2 || undefined } : null;
    case 'email':
      return f1.trim() ? { kind, address: f1, subject: f2 || undefined, body: f3 || undefined } : null;
    case 'geo': {
      const lat = parseFloat(f1);
      const lon = parseFloat(f2);
      return Number.isFinite(lat) && Number.isFinite(lon) ? { kind, lat, lon } : null;
    }
    case 'social':
      return f1.trim() ? { kind, network: social, username: f1 } : null;
    case 'wifi':
      return f1.trim() ? { kind, ssid: f1, password: f2 || undefined, auth: wifiAuth } : null;
    case 'vcard':
      return f1.trim() ? { kind, name: f1, phone: f2 || undefined, email: f3 || undefined } : null;
    case 'video':
      return f1.trim() ? { kind, url: f1 } : null;
    case 'appLink':
      return f1.trim() ? { kind, url: f1 } : null;
  }
}

const styles = StyleSheet.create({
  screen: { flex: 1 },
  modalScreen: { flex: 1 },
  content: { padding: 16, paddingBottom: 36, gap: 12 },
  hint: { color: colors.textMuted, fontSize: 13, lineHeight: 18, marginTop: -4 },
  formTitle: { fontSize: 16, fontWeight: '700', color: colors.text },
  savedRow: { gap: 4 },
  savedRowHeader: { flexDirection: 'row', alignItems: 'center', gap: 8 },
  savedTitle: { fontSize: 16, fontWeight: '700', color: colors.text, letterSpacing: 0.2 },
  subtitle: { color: colors.textMuted, fontSize: 13 },
  chipRow: { flexGrow: 0, marginVertical: 4 },
  chip: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 6,
    paddingHorizontal: 14,
    paddingVertical: 9,
    borderRadius: 20,
    backgroundColor: colors.surfaceAlt,
    borderWidth: 1,
    borderColor: colors.border,
    marginRight: 8,
  },
  chipSelected: { backgroundColor: colors.primary, borderColor: colors.primary },
  chipText: { color: colors.textMuted, fontSize: 13, fontWeight: '600' },
  chipTextSelected: { color: colors.onPrimary, fontWeight: '700' },
  input: {
    borderWidth: 1,
    borderColor: colors.border,
    backgroundColor: colors.surfaceAlt,
    borderRadius: 10,
    paddingHorizontal: 12,
    paddingVertical: 11,
    marginVertical: 4,
    color: colors.text,
  },
  recordRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    backgroundColor: colors.surfaceAlt,
  },
  recordRowLeft: { flexDirection: 'row', alignItems: 'center', gap: 10, flex: 1 },
  recordText: { flex: 1, color: colors.text, fontWeight: '500' },
  removeButton: {
    width: 28,
    height: 28,
    borderRadius: 14,
    alignItems: 'center',
    justifyContent: 'center',
    backgroundColor: colors.dangerMuted,
  },
  status: { color: colors.textMuted },
  statusSuccess: { color: colors.success, fontWeight: '600' },
  statusError: { color: colors.danger, fontWeight: '600' },
  buttonRow: { flexDirection: 'row', gap: 12, marginTop: 8 },
});
