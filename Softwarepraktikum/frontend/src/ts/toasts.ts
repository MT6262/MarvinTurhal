import {ref, type Ref} from "vue";
import type {IconDefinition} from "@fortawesome/free-solid-svg-icons";
import {faInfo, faTimes} from "@fortawesome/free-solid-svg-icons";

type ToastType = "info" | "success" | "error" | "warning" | "dark";

export class Toast {
    title: string;
    message: string;
    icon: IconDefinition;
    type: ToastType;
    expired = false;
    key = Math.random();
    persistent: boolean;
    className?: string;

    constructor(
        title: string,
        message: string,
        type: ToastType | null = null,
        icon: IconDefinition | null = null,
        timeout: number = 5,
        persistent: boolean = false,
        className?: string
    ) {
        this.title = title;
        this.message = message;
        this.icon = icon || faInfo;
        this.type = type || "info";
        this.persistent = persistent;
        this.className = className;

        if (!persistent && timeout > 0) {
            setTimeout(() => this.close(), timeout * 1_000);
        }
    }

    close() {
        this.expired = true;
        // Wenn es der Workshop-Toast ist, speichere den Status
        if (this.className === 'next-workshop-toast') {
            localStorage.setItem('workshopToastDismissed', 'true');
        }
        activeToasts.value = activeToasts.value.filter(toast => !toast.expired);
    }
}

export const activeToasts: Ref<Toast[]> = ref([]);

export function showToast(toast: Toast) {
    // Wenn es ein persistenter Toast ist und eine Klasse hat,
    // entferne zuerst alle existierenden Toasts mit der gleichen Klasse
    if (toast.persistent && toast.className) {
        activeToasts.value = activeToasts.value.filter(t => t.className !== toast.className);
    }
    activeToasts.value.push(toast);
}
