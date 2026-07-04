import React, { useCallback, useState } from 'react';
import { Alert, Modal, ScrollView, StyleSheet, Text, TouchableOpacity, View } from 'react-native';
import { trigger } from 'react-native-haptic-feedback';
import { Card, PrimaryButton, SecondaryButton, SectionHeader } from '../components/Primitives';
import { ScreenBackground } from '../components/ScreenBackground';
import { Icon } from '../components/Icons';
import { addHistoryEntry, getProfileById, saveProfile } from '../data/storage';
import { writeRecords } from '../nfc/NfcService';
import { runChain } from '../tasks/executor';
import { templatesByCategory } from '../tasks/catalog';
import { taskChainToRecord } from '../tasks/taskChainNdef';
import { StepResult, TaskActionSpec, TaskStep, actionLabel } from '../tasks/types';
import { colors } from '../theme/colors';

export default function TasksScreen() {
  const [steps, setSteps] = useState<TaskStep[]>([]);
  const [pickerOpen, setPickerOpen] = useState(false);
  const [results, setResults] = useState<StepResult[] | null>(null);
  const [writeStatus, setWriteStatus] = useState<'idle' | 'writing' | 'success' | { error: string }>('idle');

  const addStep = (action: TaskActionSpec) => {
    setSteps((prev) => [...prev, { action, condition: { kind: 'always' } }]);
    setPickerOpen(false);
  };

  const removeStep = (index: number) => setSteps((prev) => prev.filter((_, i) => i !== index));

  const runNow = useCallback(async () => {
    const runResults = await runChain({ steps }, async (id) => (await getProfileById(id))?.taskChain ?? null);
    setResults(runResults);
    await addHistoryEntry('TASK_RUN', `Ran ${steps.length} task step(s)`);
  }, [steps]);

  const saveAsProfile = useCallback(() => {
    if (steps.length === 0) return;
    Alert.prompt('Save profile', 'Profile name', (name) => {
      if (!name) return;
      saveProfile({ name, records: [], taskChain: { steps } });
    });
  }, [steps]);

  const writeToTag = useCallback(async () => {
    if (steps.length === 0) return;
    setWriteStatus('writing');
    const result = await writeRecords([taskChainToRecord({ steps })]);
    if (result.ok) {
      setWriteStatus('success');
      await addHistoryEntry('WRITE', 'Attached automation to tag');
    } else {
      setWriteStatus({ error: result.message });
    }
  }, [steps]);

  return (
    <ScreenBackground>
    <ScrollView style={styles.screen} contentContainerStyle={styles.content}>
      <SecondaryButton title="Add a task" onPress={() => setPickerOpen(true)} icon="plus" />

      {steps.length === 0 && <Text style={styles.hint}>No tasks yet. Tasks run in order, immediately or when the tag they're attached to is scanned.</Text>}

      {steps.map((step, index) => (
        <Card key={index} style={styles.stepRow}>
          <View style={styles.stepBadge}>
            <Text style={styles.stepBadgeText}>{index + 1}</Text>
          </View>
          <Text style={styles.stepText}>{actionLabel(step.action)}</Text>
          <TouchableOpacity
            style={styles.removeButton}
            onPress={() => {
              trigger('impactLight');
              removeStep(index);
            }}
            hitSlop={{ top: 8, bottom: 8, left: 8, right: 8 }}
          >
            <Icon name="close" color={colors.danger} size={15} />
          </TouchableOpacity>
        </Card>
      ))}

      {results && (
        <Card>
          <Text style={styles.formTitle}>Last run</Text>
          {results.map((result, index) => (
            <Text key={index} style={styles.resultLine}>
              • {actionLabel(result.action)}: {result.kind === 'skipped' ? `skipped (${result.reason})` : result.outcome.ok ? 'done' : result.outcome.reason}
            </Text>
          ))}
        </Card>
      )}

      {writeStatus === 'writing' && <Text style={styles.hint}>Hold your iPhone near the tag…</Text>}
      {writeStatus === 'success' && <Text style={styles.success}>Automation attached to tag.</Text>}
      {typeof writeStatus === 'object' && <Text style={styles.error}>Failed: {writeStatus.error}</Text>}

      <View style={styles.buttonRow}>
        <SecondaryButton title="Save" onPress={saveAsProfile} disabled={steps.length === 0} icon="starOutline" />
        <SecondaryButton title="Run now" onPress={runNow} disabled={steps.length === 0} icon="play" />
        <PrimaryButton title="Write to tag" onPress={writeToTag} disabled={steps.length === 0} icon="download" />
      </View>

      <TaskPickerModal visible={pickerOpen} onClose={() => setPickerOpen(false)} onPick={addStep} />
    </ScrollView>
    </ScreenBackground>
  );
}

function TaskPickerModal({
  visible,
  onClose,
  onPick,
}: {
  visible: boolean;
  onClose: () => void;
  onPick: (action: TaskActionSpec) => void;
}) {
  return (
    <Modal visible={visible} animationType="slide" onRequestClose={onClose} presentationStyle="pageSheet">
      <ScreenBackground>
      <ScrollView style={styles.modalScreen} contentContainerStyle={styles.content}>
        <SectionHeader title="Add a task" />
        {templatesByCategory().map(([category, templates]) => (
          <View key={category}>
            <Text style={styles.categoryHeader}>{category.replace('_', ' ')}</Text>
            {templates.map((template) => (
              <Card key={template.title} style={styles.templateCard}>
                <TouchableOpacity
                  style={styles.templateRow}
                  onPress={() => {
                    trigger('selection');
                    onPick(template.default);
                  }}
                  activeOpacity={0.7}
                >
                  <View style={styles.templateTextGroup}>
                    <Text style={styles.templateTitle}>{template.title}</Text>
                    <Text style={styles.templateDescription}>{template.description}</Text>
                  </View>
                  <Icon name="chevronRight" color={colors.textFaint} size={16} />
                </TouchableOpacity>
              </Card>
            ))}
          </View>
        ))}
        <SecondaryButton title="Close" onPress={onClose} icon="close" />
      </ScrollView>
      </ScreenBackground>
    </Modal>
  );
}

const styles = StyleSheet.create({
  screen: { flex: 1 },
  modalScreen: { flex: 1 },
  content: { padding: 16, paddingBottom: 36, gap: 12 },
  hint: { color: colors.textMuted, lineHeight: 20 },
  stepRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 10,
    backgroundColor: colors.surfaceAlt,
  },
  stepBadge: {
    width: 24,
    height: 24,
    borderRadius: 12,
    alignItems: 'center',
    justifyContent: 'center',
    backgroundColor: colors.primaryMuted,
  },
  stepBadgeText: { color: colors.primary, fontWeight: '700', fontSize: 12 },
  stepText: { flex: 1, color: colors.text, fontWeight: '500' },
  removeButton: {
    width: 28,
    height: 28,
    borderRadius: 14,
    alignItems: 'center',
    justifyContent: 'center',
    backgroundColor: colors.dangerMuted,
  },
  formTitle: { fontSize: 16, fontWeight: '700', color: colors.text },
  resultLine: { color: colors.text },
  success: { color: colors.success, fontWeight: '600' },
  error: { color: colors.danger, fontWeight: '600' },
  buttonRow: { flexDirection: 'row', gap: 8, marginTop: 8 },
  categoryHeader: {
    fontWeight: '700',
    color: colors.primary,
    marginTop: 14,
    marginBottom: 4,
    textTransform: 'uppercase',
    fontSize: 12,
    letterSpacing: 1,
  },
  templateCard: { marginBottom: 8, backgroundColor: colors.surfaceAlt },
  templateRow: { flexDirection: 'row', alignItems: 'center', gap: 10 },
  templateTextGroup: { flex: 1, gap: 2 },
  templateTitle: { fontWeight: '700', color: colors.text },
  templateDescription: { color: colors.textMuted, fontSize: 12, marginTop: 2 },
});
