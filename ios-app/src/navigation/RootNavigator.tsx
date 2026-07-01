import React from 'react';
import { NavigationContainer } from '@react-navigation/native';
import { createBottomTabNavigator } from '@react-navigation/bottom-tabs';
import { createNativeStackNavigator } from '@react-navigation/native-stack';
import { Text } from 'react-native';
import ReadScreen from '../screens/ReadScreen';
import WriteScreen from '../screens/WriteScreen';
import TasksScreen from '../screens/TasksScreen';
import MoreScreen from '../screens/MoreScreen';
import ProfilesScreen from '../screens/ProfilesScreen';
import HistoryScreen from '../screens/HistoryScreen';
import { colors } from '../theme/colors';

const Tab = createBottomTabNavigator();
const MoreStack = createNativeStackNavigator();

function TabIcon({ emoji }: { emoji: string }) {
  return <Text style={{ fontSize: 20 }}>{emoji}</Text>;
}

function MoreStackNavigator() {
  return (
    <MoreStack.Navigator screenOptions={{ headerTintColor: colors.primary }}>
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
    <NavigationContainer>
      <Tab.Navigator screenOptions={{ tabBarActiveTintColor: colors.primary }}>
        <Tab.Screen
          name="Read"
          component={ReadScreen}
          options={{ tabBarIcon: () => <TabIcon emoji="📡" />, headerTintColor: colors.primary }}
        />
        <Tab.Screen
          name="Write"
          component={WriteScreen}
          options={{ tabBarIcon: () => <TabIcon emoji="✏️" />, headerTintColor: colors.primary }}
        />
        <Tab.Screen
          name="Tasks"
          component={TasksScreen}
          options={{ tabBarIcon: () => <TabIcon emoji="⚡" />, headerTintColor: colors.primary }}
        />
        <Tab.Screen name="More" component={MoreStackNavigator} options={{ tabBarIcon: () => <TabIcon emoji="⋯" />, headerShown: false }} />
      </Tab.Navigator>
    </NavigationContainer>
  );
}
