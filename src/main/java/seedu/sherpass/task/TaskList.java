package seedu.sherpass.task;

import seedu.sherpass.enums.Frequency;
import seedu.sherpass.util.Ui;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Random;

import static seedu.sherpass.constant.Message.EMPTY_STRING;
import static seedu.sherpass.constant.Message.ERROR_SCHEDULE_CLASH_MESSAGE;

public class TaskList {
    private ArrayList<Task> tasks;
    private HashSet<Integer> identifierList;

    /**
     * Creates a constructor for the class TaskList.
     *
     * @param savedTasks Representation of an array of tasks.
     */
    public TaskList(ArrayList<Task> savedTasks) {
        tasks = savedTasks;
        identifierList = new HashSet<>();
        refreshIdentifier();
    }

    public TaskList() {
        identifierList = new HashSet<>();
        tasks = new ArrayList<>();
    }

    /**
     * Returns the array of tasks in the class TaskList.
     *
     * @return the array of tasks.
     */
    public ArrayList<Task> getTasks() {
        return tasks;
    }

    /**
     * Returns the incremented date according to the frequency.
     *
     * @param currentDate The current date to be incremented
     * @param frequency   The frequency of recurrence.
     * @return The incremented date according to the frequency
     */
    private static LocalDateTime incrementDate(LocalDateTime currentDate, Frequency frequency) {
        if (frequency == Frequency.DAILY) {
            return currentDate.plusDays(1);
        } else if (frequency == Frequency.WEEKLY) {
            return currentDate.plusWeeks(1);
        }
        return currentDate.plusMonths(1);
    }

    /**
     * Returns the last recurrence of a recurring task.
     * For tasks recurring daily, the last date is 1 month after the first occurrence.
     * For tasks recurring weekly, the last date is 2 month after the first occurrence.
     * For tasks recurring monthly, the last date is 1 year after the first occurrence.
     *
     * @param startDate The date of the first occurrence
     * @param frequency The frequency of recurrence
     * @return The date of the last recurrence for a recurring task.
     */
    private static LocalDateTime getEndDateForRecurrence(LocalDateTime startDate, Frequency frequency) {
        if (frequency == Frequency.DAILY) {
            return startDate.plusMonths(1);
        } else if (frequency == Frequency.WEEKLY) {
            return startDate.plusMonths(2);
        }
        return startDate.plusYears(1);
    }

    /**
     * Returns the next occurrence of a recurring task.
     *
     * @param currentTask The i-th task
     * @return The i+1 task
     */
    private static Task prepareNextTask(Task currentTask) {
        LocalDateTime newStartDate = incrementDate(currentTask.getDoOnStartDateTime(),
                currentTask.getRepeatFrequency());
        LocalDateTime newEndDate = incrementDate(currentTask.getDoOnEndDateTime(),
                currentTask.getRepeatFrequency());
        return new Task(currentTask.getIdentifier(), currentTask.getDescription(), null,
                newStartDate, newEndDate, currentTask.getRepeatFrequency(), 0);
    }


    private boolean hasDateTimeClash(Task newTask) {
        for (Task task : tasks) {
            boolean isOnSameDay = (task.getDoOnDateString(true)
                    .equals(newTask.getDoOnDateString(true)));
            boolean hasTimeClash = (newTask.getDoOnStartDateTime().isEqual(task.getDoOnStartDateTime())
                    || (newTask.getDoOnEndDateTime().isAfter(task.getDoOnStartDateTime())
                    && newTask.getDoOnStartDateTime().isBefore(task.getDoOnStartDateTime()))
                    || (newTask.getDoOnStartDateTime().isBefore(task.getDoOnEndDateTime())
                    && newTask.getDoOnEndDateTime().isAfter(task.getDoOnEndDateTime()))
                    || (newTask.getDoOnStartDateTime().isAfter(task.getDoOnStartDateTime())
                    && newTask.getDoOnEndDateTime().isBefore(task.getDoOnEndDateTime())))
                    || (task.getDoOnStartDateTime().isAfter(newTask.getDoOnStartDateTime())
                    && task.getDoOnEndDateTime().isBefore(newTask.getDoOnEndDateTime()));
            if (isOnSameDay && hasTimeClash) {
                return true;
            }
        }
        return false;
    }


    private void updateIndex() {
        int i = 1;
        for (Task task : tasks) {
            task.setIndex(i);
            i++;
        }
    }

    /**
     * Adds a new task to the current array of tasks.
     *
     * @param newTask The new task to be added to the array.
     * @param isRepeat Checks if task repeats.
     * @param ui User Interface.
     */
    public void addTask(Task newTask, boolean isRepeat, Ui ui) {
        String repeatKeyWord = EMPTY_STRING;
        if (hasDateTimeClash(newTask)) {
            ui.showToUser(ERROR_SCHEDULE_CLASH_MESSAGE);
            return;
        }
        if (!isRepeat) {
            tasks.add(newTask);
        } else {
            repeatKeyWord = "repeated";
            identifierList.add(newTask.getIdentifier());
            LocalDateTime lastRecurrenceDate = getEndDateForRecurrence(newTask.getDoOnStartDateTime(),
                    newTask.getRepeatFrequency());
            ArrayList<Task> taskListToAdd = new ArrayList<>();
            do {
                if (hasDateTimeClash(newTask)) {
                    ui.showToUser(ERROR_SCHEDULE_CLASH_MESSAGE);
                    return;
                }
                taskListToAdd.add(newTask);
                newTask = prepareNextTask(newTask);
            } while (newTask.getDoOnStartDateTime().isBefore(lastRecurrenceDate));
            tasks.addAll(taskListToAdd);
        }
        tasks.sort(new TaskDateComparator());
        updateIndex();
        ui.showToUser("Got it! I've added this " + repeatKeyWord + " task:\n   "
                + newTask + "\n"
                + "Now you have " + tasks.size() + " task(s) in your schedule!");
    }

    /**
     * Prints all available tasks in the task list.
     *
     * @param ui Ui class for printing of messages.
     */
    public void printAllTasks(Ui ui) {
        int printIndex = 1;
        System.out.println("Here are the tasks in your list:");
        for (Task task : tasks) {
            ui.showToUser(printIndex + ". " + task);
            printIndex++;
        }
        ui.showLine();
        ui.showToUser("You have " + (printIndex - 1) + " task(s) in your list.");
    }


    /**
     * Returns a boolean value denoting the task status, i.e.
     * whether the task has been marked or not.
     *
     * @param markIndex Index of a task to check for its mark status.
     * @return Returns true if task has been marked. False otherwise.
     */
    public boolean isTaskDone(int markIndex) {
        return tasks.get(markIndex).isDone();
    }


    /**
     * Marks a task given the index of the task.
     * Index corresponds to its placement within the task array.
     *
     * @param markIndex Index of the task to mark as done.
     * @param ui User Interface.
     */
    public void markTask(int markIndex, Ui ui) {
        tasks.get(markIndex).markAsDone();
        ui.showToUser("Nice! I've marked this task as done:\n  " + tasks.get(markIndex));
    }


    /**
     * Unmarks a task given the index of the task.
     * Index corresponds to its placement within the task array.
     *
     * @param markIndex Index of the task to mark as undone.
     * @param ui User Interface.
     */
    public void unmarkTask(int markIndex, Ui ui) {
        tasks.get(markIndex).markAsUndone();
        ui.showToUser("Ok, I've marked this task as" + " not done yet:\n  " + tasks.get(markIndex));
    }

    /**
     * Returns a boolean value denoting the existence of a task
     * within the task array.
     *
     * @param index Index of a task. Corresponds to its placement in task array.
     * @return Returns true if task exists in task array. False otherwise.
     */
    public boolean isTaskExist(int index) {
        return index >= 0 && index < tasks.size();
    }

    public int getSize() {
        return tasks.size();
    }

    public Task getTask(int index) {
        return tasks.get(index);
    }

    /**
     * Deletes a task given its index. Index corresponds to its placement
     * in task array.
     *
     * @param deleteIndex Index of a task to search for.
     */
    public void removeTask(int deleteIndex) {
        Task taskToBeRemoved = tasks.get(deleteIndex);
        tasks.remove(deleteIndex);
        System.out.println("Okay. I've removed this task:\n  " + taskToBeRemoved
                + "\nNow you have " + tasks.size() + " task(s) in the list.");
    }

    /**
     * Deletes all tasks saved within the task array.
     *
     * @param ui Ui for printing the completion of the deletion.
     */
    public void deleteAllTasks(Ui ui) {
        while (tasks.size() > 0) {
            tasks.remove(0);
        }
        ui.showLine();
        ui.showToUser("Done! Now you have " + tasks.size() + " task(s) in the list.");
    }

    private void refreshIdentifier() {
        for (Task t : tasks) {
            identifierList.add(t.getIdentifier());
        }
    }

    public int generateIdentifier() {
        Random generator = new Random();
        int candidate;
        do {
            candidate = generator.nextInt(65536);
        } while (identifierList.contains(candidate));
        return candidate;
    }

    /**
     * Return a filtered ArrayList of task according to the date specified.
     *
     * @param dateInput The specific date.
     * @return The filtered ArrayList.
     */
    public ArrayList<Task> getFilteredTasksByDate(LocalDate dateInput) {
        ArrayList<Task> filteredTasks = new ArrayList<>();
        for (Task task : tasks) {
            if (hasDoOnDate(task) && task.getDoOnStartDateTime().toLocalDate().isEqual(dateInput)) {
                filteredTasks.add(task);
            }
        }
        return filteredTasks;
    }

    private boolean hasDoOnDate(Task task) {
        return task.getDoOnStartDateTime() != null;
    }

    /**
     * Prints tasks that are yet to be completed, i.e. marked as done.
     * Printed tasks applies to non-recurring tasks.
     *
     * @param ui User interface
     */
    public void printPendingTasks(Ui ui) {
        int printIndex = 1;
        for (Task task : tasks) {
            if (!task.isDone()) {
                ui.showToUser(printIndex + ". " + task);
                printIndex++;
            }
        }
    }

    public int getPendingTasksCount() {
        int count = 0;
        for (Task task : tasks) {
            if (!task.isDone()) {
                count++;
            }
        }
        return count;
    }
}
