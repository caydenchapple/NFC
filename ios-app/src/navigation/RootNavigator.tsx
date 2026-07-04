import React from 'react';
import { DarkTheme, NavigationContainer, Theme } from '@react-navigation/native';
import { createBottomTabNavigator } from '@react-navigation/bottom-tabs';
import { createNativeStackNavigator } from '@react-navigation/native-stack';
import { StyleSheet, View } from 'react-native';
import ReadScreen from '../screens/ReadScreen';
import WriteScreen from '../screens/WriteScreen';
import TasksScreen from '../screens/TasksScreen';
import MoreScreen from '../screens/MoreScreen';
import ProfilesScreen from '../screens/ProfilesScreen';
import HistoryScreen from '../screens/HistoryScreen';
import { colors } from '../theme/colors';
import { Icon, IconName } from '../components/Icons';

const Tab = createBottomTabNavigator();
const MoreStack = createNativeStackNavigator();

const navTheme: Theme = {
  ...DarkTheme,
  colors: {
    ...DarkTheme.colors,
    primary: colors.primary,
    background: colors.background,
    card: colors.surface,
    text: colors.text,
    border: colors.border,
    notification: colors.primary,
  },
};

const headerOptions = {
  headerStyle: { backgroundColor: colors.surface },
  headerTintColor: colors.primary,
  headerTitleStyle: { color: colors.text, fontWeight: '700' as const, fontSize: 17, letterSpacing: 0.3 },
  headerShadowVisible: false,
};

function TabIcon({ name, focused }: { name: IconName; focused: boolean }) {
  return (
    <View style={[styles.tabIconWrap, focused && styles.tabIconWrapActive]}>
      <Icon name={name} color={focused ? colors.primary : colors.textFaint} size={19} />
    </View>
  );
}

function MoreStackNavigator() {
  return (
    <MoreStack.Navigator screenOptions={headerOptions}>
      <MoreStack.Screen name="MoreRoot" options={{ title: 'More' }}>
        {({ navigation }) => (
          <MoreScreen
            onOpenProfiles={() => navigation.navigate('Profiles')}
            onOpenHistory={() => navigation.navigate('History')}
          />
        )}
      </MoreStack.Screen>
      <MoreStack.Screen name="Profiles" component={ProfilesScreen} options={{ title: 'Profiles' }} />
      <MoreStack.Screen name="History" component={HistoryScreen} options={{ title: 'History' }} />
    </MoreStack.Navigator>
  );
}

export default function RootNavigator() {
  return (
    <NavigationContainer theme={navTheme}>
      <Tab.Navigator
        screenOptions={{
          ...headerOptions,
          tabBarActiveTintColor: colors.primary,
          tabBarInactiveTintColor: colors.textFaint,
          tabBarStyle: styles.tabBar,
          tabBarLabelStyle: styles.tabBarLabel,
        }}
      >
        <Tab.Screen
          name="Read"
          component={ReadScreen}
          options={{ tabBarIcon: ({ focused }) => <TabIcon name="scan" focused={focused} /> }}
        />
        <Tab.Screen
          name="Write"
          component={WriteScreen}
          options={{ tabBarIcon: ({ focused }) => <TabIcon name="pencil" focused={focused} /> }}
        />
        <Tab.Screen
          name="Tasks"
          component={TasksScreen}
          options={{ tabBarIcon: ({ focused }) => <TabIcon name="gear" focused={focused} /> }}
        />
        <Tab.Screen
          name="More"
          component={MoreStackNavigator}
          options={{ tabBarIcon: ({ focused }) => <TabIcon name="dots" focused={focused} />, headerShown: false }}
        />
      </Tab.Navigator>
    </NavigationContainer>
  );
}

const styles = StyleSheet.create({
  tabBar: {
    backgroundColor: colors.surface,
    borderTopColor: colors.border,
    borderTopWidth: 1,
    height: 88,
    paddingTop: 8,
  },
  tabBarLabel: {
    fontSize: 11,
    fontWeight: '600',
  },
  tabIconWrap: {
    width: 36,
    height: 30,
    borderRadius: 12,
    alignItems: 'center',
    justifyContent: 'center',
  },
  tabIconWrapActive: {
    backgroundColor: colors.primaryMuted,
  },
});
