// store.ts
import { create } from 'zustand';

const useStore = create(set => ({
	parsedList: [],
	setParsedList: list => set({ parsedList: list }),
	detectedList: [],
	setDetectedList: list => set({ detectedList: list }),
	checkedList: [],
	setCheckedList: list => set({ checkedList: list }),
	dispatchedList: [],
	setDispatchedList: list => set({ dispatchedList: list }),
	selectedRows: [],
	setSelectedRows: rows => set({ selectedRows: rows }),
	jobClusterId: '',
	setJobClusterId: id => set({ jobClusterId: id })
}));

export default useStore;
