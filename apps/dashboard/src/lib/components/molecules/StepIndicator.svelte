<script lang="ts">
  interface Props {
    steps: string[]
    currentStep: number // 1-based
  }

  const { steps, currentStep }: Props = $props()
</script>

<nav aria-label="Progression du formulaire">
  <ol class="flex items-center w-full">
    {#each steps as step, i (step)}
      {@const stepNumber = i + 1}
      {@const isCompleted = stepNumber < currentStep}
      {@const isCurrent = stepNumber === currentStep}

      <li class="flex flex-1 items-center {i < steps.length - 1 ? 'after:flex-1 after:h-px after:mx-2 after:content-[\'\'] after:border-t after:border-gray-200' : ''}">
        <div class="flex flex-col items-center gap-1">
          <!-- Circle -->
          <div
            class="flex h-7 w-7 shrink-0 items-center justify-center rounded-full text-xs font-semibold transition-default
              {isCompleted
                ? 'bg-wakeve-600 text-white'
                : isCurrent
                  ? 'ring-2 ring-wakeve-600 text-wakeve-600 bg-white'
                  : 'bg-gray-100 text-gray-400'}"
            aria-current={isCurrent ? 'step' : undefined}
          >
            {#if isCompleted}
              <!-- Checkmark -->
              <svg
                xmlns="http://www.w3.org/2000/svg"
                viewBox="0 0 20 20"
                fill="currentColor"
                class="h-4 w-4"
                aria-hidden="true"
              >
                <path
                  fill-rule="evenodd"
                  d="M16.704 4.153a.75.75 0 0 1 .143 1.052l-8 10.5a.75.75 0 0 1-1.127.075l-4.5-4.5a.75.75 0 0 1 1.06-1.06l3.894 3.893 7.48-9.817a.75.75 0 0 1 1.05-.143Z"
                  clip-rule="evenodd"
                />
              </svg>
            {:else}
              {stepNumber}
            {/if}
          </div>

          <!-- Label -->
          <span
            class="text-xs text-center max-w-[5rem] leading-tight
              {isCurrent ? 'text-wakeve-600 font-medium' : isCompleted ? 'text-gray-700' : 'text-gray-400'}"
          >
            {step}
          </span>
        </div>

        <!-- Connector line (between circles, not after last) -->
        {#if i < steps.length - 1}
          <div
            class="flex-1 mx-2 h-px transition-default
              {isCompleted ? 'bg-wakeve-600' : 'bg-gray-200'}"
            aria-hidden="true"
          ></div>
        {/if}
      </li>
    {/each}
  </ol>
</nav>
