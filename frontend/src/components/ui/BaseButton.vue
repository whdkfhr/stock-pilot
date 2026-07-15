<script setup lang="ts">
withDefaults(
  defineProps<{
    variant?: 'primary' | 'secondary' | 'ghost'
    size?: 'md' | 'lg'
    type?: 'button' | 'submit'
    disabled?: boolean
    loading?: boolean
    block?: boolean
  }>(),
  { variant: 'primary', size: 'lg', type: 'button', disabled: false, loading: false, block: true },
)
</script>

<template>
  <button
    :type="type"
    :disabled="disabled || loading"
    :class="['btn', `btn--${variant}`, `btn--${size}`, { 'btn--block': block }]"
  >
    <span v-if="loading" class="btn__spinner" />
    <slot v-else />
  </button>
</template>

<style scoped>
.btn {
  border: none;
  border-radius: var(--radius-md);
  font-weight: 600;
  letter-spacing: -0.01em;
  transition: background 0.15s ease, transform 0.05s ease, opacity 0.15s ease;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: var(--space-2);
}
.btn--block {
  width: 100%;
}
.btn--md {
  height: 44px;
  padding: 0 16px;
  font-size: 15px;
}
.btn--lg {
  height: 54px;
  padding: 0 20px;
  font-size: 17px;
}
.btn:active:not(:disabled) {
  transform: scale(0.99);
}
.btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}
.btn--primary {
  background: var(--color-primary);
  color: var(--color-text-onprimary);
}
.btn--primary:hover:not(:disabled) {
  background: var(--color-primary-hover);
}
.btn--secondary {
  background: var(--color-primary-weak);
  color: var(--color-primary);
}
.btn--ghost {
  background: transparent;
  color: var(--color-text-sub);
}
.btn--ghost:hover:not(:disabled) {
  background: var(--color-surface-hover);
}
.btn__spinner {
  width: 18px;
  height: 18px;
  border: 2px solid rgba(255, 255, 255, 0.4);
  border-top-color: #fff;
  border-radius: 50%;
  animation: spin 0.6s linear infinite;
}
@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}
</style>
