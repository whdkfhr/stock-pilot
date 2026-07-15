<script setup lang="ts">
defineProps<{
  label?: string
  type?: string
  placeholder?: string
  autocomplete?: string
  error?: string
}>()

const model = defineModel<string>()
</script>

<template>
  <label class="field">
    <span v-if="label" class="field__label">{{ label }}</span>
    <input
      v-model="model"
      :type="type ?? 'text'"
      :placeholder="placeholder"
      :autocomplete="autocomplete"
      :class="['field__input', { 'field__input--error': error }]"
    />
    <span v-if="error" class="field__error">{{ error }}</span>
  </label>
</template>

<style scoped>
.field {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
}
.field__label {
  font-size: 14px;
  font-weight: 600;
  color: var(--color-text-sub);
  padding-left: 2px;
}
.field__input {
  height: 54px;
  padding: 0 16px;
  border: 1.5px solid var(--color-border);
  border-radius: var(--radius-md);
  background: var(--color-surface);
  font-size: 16px;
  color: var(--color-text-strong);
  transition: border-color 0.15s ease;
}
.field__input::placeholder {
  color: var(--color-text-tertiary);
}
.field__input:focus {
  outline: none;
  border-color: var(--color-primary);
}
.field__input--error {
  border-color: var(--color-danger);
}
.field__error {
  font-size: 13px;
  color: var(--color-danger);
  padding-left: 2px;
}
</style>
