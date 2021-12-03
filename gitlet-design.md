
![](../../../Downloads/Git-Logo-White.png)

# Gitlet Design Document

**Author**: *Jake Clayton*

___
## Classes and Data Structures

### Main:
Handles taking in arguments and processing them and initializes persistence

***Variables:***
- `CWD` – the command working directory
- `GITLET_FOLDER` – The .gitlet repository where all persistence is stored
- `TRACKING_FOLDER` – The tracking folder where the information for the most current commit and the active branch are stored
- `STAGING_AREA` – The staging area folder where items are sent when there are staged to be added or removed


### StagingArea:
Representation of the staging area, where files are staged to be added or removed, saved in a subdirectory of .gitlet


***Variables:***
  - `STAGING_FOLDER` – parent folder (under .gitlet) containing the following two subfolders
  - `ADDITION_FOLDER` – where files are staged to be committed 
  - `REMOVAL_FOLDER` – where files are staged to be removed


### CommitTree:


***Variables:***

### Commit:

Representation of a commit, with a pointer to its parent. Includes file information stored. 

***Variables:***
- `_message` – log message the user typed in
- `_timestamp` – exact time this commit was created
- `_hashCode` – uses the sha-1 function to make a unique id string out of the timestamp and log message
- `_parent` – pointer to the previous commit, the current commit's "parent"

### Blob:
Representation of the contents of a file. Should use this to handle encapsulation, be able to convert back and forth.

***Variables***
- `_contents` – contents of the file (maybe encapsulated)
- `_name` – name of the file (might not be necessary)
- `_hashCode` – uses the sha-1 function to make an id using the contents of the file 
___
## Algorithms

###Class Main

1. `main(String... args)` 
    - takes in git commands and appropriately abstracts away the details to other methods of Main and other classes.
    - has proper exit messages for incorrect input commands
2. `getDirectory()`
   - returns the .gitlet directory under the current working directory
3. `setupDirectory()`
   - creates the pathways and the files for the .gitlet repository, the staging area, and the tracking file.
4. `exitWithError(String message) `
   - exits with code 0, prints out an appropriate error message
   - Modified the lab11 exit error function to use in gitlet, might not need this and can just use the one given in Utils.java
5. `displayStatus()`
   - Prints out what branches currently exist, and marks the current branch with a *.
   - Also prints what files have been staged for addition or removal
   - for git Status command
###Class StagingArea
1. `StagingArea()`
   - initializes the staging directory, creating the necessary file pathways
2. `add(File file)`
   - Stages a file to be added 
   - uses helper method `processAdd()`
3. `processAdd(File file, File parent)`
   - Stages a file to be added
   - If the file is a directory, this recursively goes through the file and processes each subdirectory/subfiles
   - If the file is already staged to be added and its staged version is identical to its current version, the file is unstaged and removed from the staging area (might need to double check if this works properly for directories as well as pure files)
4. `isEmpty()`
   - returns true if the staging area is empty
5. `clear()`
   - clears the staging area of all files to be added or removed
6. `remove(File file)`
   - stages a file to be removed

###Class CommitTree
1. `CommitTree()`
   - not sure if this constructor is necessary
2. `addCommit(String message)`
   - does most of the work for the behavior of the `git add` command
   - handles creating a new commit, changes the pointer of the head commit, clears the staging area, updates what files are being tracked
3. `initCommit()`
   - special case function used when creating the `.gitlet` directory when creating the first commit.
4. `getHead()`
   - returns the current head commit

###Class Commit
1. `Commit(String message, HashMap contents, HashSet files, String parent)`
   - constructor function for a typical commit 
   - hashcode is based on time of commit plus the commit ID
   - contains a parent pointer, tracks a set a files in the CWD, and contains pointers to Blobs
2. `Commit(String message, HashMap contents, HashSet files, String parent, String parent2)`
   - special case constructor for merge commits
3. `Commit()`
   - creates the first commit when initializing the .gitlet repository, special time signature
4. `get_hashCode()`
   - returns the sha-1 ID given to the commit at the time of its initialization
5. `toString()`
   - String output for the **log** command, containing the necessary instance variable information about a commit
6. `startTracking(File file)`
   - starts tracking a file, to be used when a file is staged to be added but is not already tracked

###Class Blob
1. `Blob(File file)`
   - creates a representation of the file's contents (only pure files not directories)
2. `get_hashCode()`
   - returns the sha-1 id String

___
## Persistence

The commit tree history and the staging area for files to be added or removed is stored inside a `.gitlet` folder. 
Will also store the pointers to the active branch and head commit inside a seperate file called `tracking`, might use a data structure for that like an ArrayList.

The classes `Main` and `StagingArea` do most of the work for persistence and saving files; Class `Blob` is used for encoding files to be retrieved if necessary later.

When `init` is called, the `.gitlet` directory is created, and the staging area directory and tracking file are created within .gitlet. When **add** is called, a snapshot of the file is staged to be added, saved inside the staging folder. 
When `commit` is called, a new commit is created and saved inside `.gitlet`, in the process deleting the contents of the staging folder.
When `rm-branch` or `branch` is called, appropriate changes are made to `tracking`. 

Will create a hashmap with commit ID's (or abbreviated ID's) as the keys and pointers to commits as the values to enable finding commits based on their ID's in O(N) time. 

Will add more information for `checkout`, `reset`, and `merge`.