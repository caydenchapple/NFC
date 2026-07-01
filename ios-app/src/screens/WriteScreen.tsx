import React, { useCallback, useState } from 'react';
import { Alert, ScrollView, StyleSheet, Text, TextInput, TouchableOpacity, View } from 'react-native';
import { Card, PrimaryButton, SecondaryButton, SectionHeader } from '../components/Primitives';
import { saveProfile } from '../data/storage';
import { addHistoryEntry } from '../data/storage';
import { writeRecords } from '../nfc/NfcService';
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

const SOCIAL_NETWORKS: SocialNetwork[] = ['INSTAGRAM', 'X_TWITTER', 'FACEBOOK', 'LINKEDIN', 'TIKTOK', 'YOUTUBE', 'GITHUB', 'WHATSAPP'];
const WIFI_AUTH_TYPES: WifiAuthType[] = ['OPEN', 'WPA_PERSONAL', 'WPA2_PERSONAL'];

export default function WriteScreen() {
  const [records, setRecords] = useState<RecordSpec[]>([]);
  const [status, setStatus] = useState<'idle' | 'writing' | 'success' | { error: string }>('idle');

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
    <ScrollView style={styles.screen} contentContainerStyle={styles.content}>
      <AddRecordForm onAdd={(spec) => setRecords((prev) => [...prev, spec])} />

      {records.length > 0 && (
        <>
          <SectionHeader title="Records to write" />
          {records.map((spec, index) => (
            <Card key={index} style={styles.recordRow}>
              <Text style={styles.recordText}>{recordLabel(spec)}</Text>
              <TouchableOpacity onPress={() => setRecords((prev) => prev.filter((_, i) => i !== index))}>
                <Text style={styles.remove}>✕</Text>
              </TouchableOpacity>
            </Card>
          ))}
        </>
      )}

      {status === 'writing' && <Text style={styles.status}>Hold your iPhone near the tag…</Text>}
      {status === 'success' && <Text style={styles.statusSuccess}>Written successfully.</Text>}
      {typeof status === 'object' && <Text style={styles.statusError}>Failed: {status.error}</Text>}

      <View style={styles.buttonRow}>
        <SecondaryButton title="Save as profile" onPress={saveAsProfile} disabled={records.length === 0} />
        <PrimaryButton title="Write to tag" onPress={write} disabled={records.length === 0} />
      </View>
    </ScrollView>
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

      <PrimaryButton title="Add record" onPress={add} />
    </Card>
  );
}

function ChipRow<T extends string>({
  options,
  selected,
  labelFor,
  onSelect,
}: {
  options: T[];
  selected: T;
  labelFor: (option: T) => string;
  onSelect: (option: T) => void;
}) {
  return (
    <ScrollView horizontal showsHorizontalScrollIndicator={false} style={styles.chipRow}>
      {options.map((option) => (
        <TouchableOpacity
          key={option}
          style={[styles.chip, option === selected && styles.chipSelected]}
          onPress={() => onSelect(option)}
        >
          <Text style={[styles.chipText, option === selected && styles.chipTextSelected]}>{labelFor(option)}</Text>
        </TouchableOpacity>
      ))}
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
  screen: { flex: 1, backgroundColor: colors.background },
  content: { padding: 16, gap: 12 },
  formTitle: { fontSize: 16, fontWeight: '600', color: colors.text },
  chipRow: { flexGrow: 0, marginVertical: 4 },
  chip: {
    paddingHorizontal: 12,
    paddingVertical: 8,
    borderRadius: 20,
    backgroundColor: colors.surfaceAlt,
    marginRight: 8,
  },
  chipSelected: { backgroundColor: colors.primary },
  chipText: { color: colors.text, fontSize: 13 },
  chipTextSelected: { color: '#fff', fontWeight: '600' },
  input: {
    borderWidth: 1,
    borderColor: colors.border,
    borderRadius: 8,
    paddingHorizontal: 12,
    paddingVertical: 10,
    marginVertical: 4,
    color: colors.text,
  },
  recordRow: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center' },
  recordText: { flex: 1, color: colors.text },
  remove: { color: colors.textMuted, fontSize: 16, paddingHorizontal: 8 },
  status: { color: colors.textMuted },
  statusSuccess: { color: colors.success },
  statusError: { color: colors.danger },
  buttonRow: { flexDirection: 'row', gap: 12, marginTop: 8 },
});
