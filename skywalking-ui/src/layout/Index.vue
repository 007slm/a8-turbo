<!-- Licensed to the Apache Software Foundation (ASF) under one or more
contributor license agreements.  See the NOTICE file distributed with
this work for additional information regarding copyright ownership.
The ASF licenses this file to You under the Apache License, Version 2.0
(the "License"); you may not use this file except in compliance with
the License.  You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License. -->
<template>
  <div class="app-wrapper flex-h">
    <SideBar v-if="notTraceRoute" />
    <div class="main-container">
      <NavBar v-if="notTraceRoute" />
      <AppMain />
    </div>
  </div>
</template>
<script lang="ts" setup>
  import { AppMain, SideBar, NavBar } from "./components";
  import { useRoute } from "vue-router";
  import { computed, onMounted } from "vue";
  import { useTheme } from "@/hooks/useTheme";

  const route = useRoute();
  const { initializeTheme } = useTheme();

  // Check if current route matches the trace route pattern
  const notTraceRoute = computed(() => {
    return !route.path.startsWith("/traces/");
  });

  // Initialize theme to preserve theme when NavBar is hidden
  onMounted(() => {
    initializeTheme();
  });
</script>
<style lang="scss" scoped>
  .app-wrapper {
    height: 100%;
  }

  .main-container {
    flex-grow: 2;
    height: 100%;
  }
</style>
