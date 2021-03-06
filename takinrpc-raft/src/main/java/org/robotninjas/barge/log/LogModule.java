/**
 * Copyright 2013 David Rusek <dave dot rusek at gmail dot com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.robotninjas.barge.log;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Throwables.propagate;

import java.io.File;
import java.io.IOException;

import javax.annotation.Nonnull;

import org.robotninjas.barge.StateMachine;

import com.google.inject.PrivateModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;

import journal.io.api.Journal;
import journal.io.api.JournalBuilder;

public class LogModule extends PrivateModule {

    private final File logDirectory;
    private final StateMachine stateMachine;

    public LogModule(@Nonnull File logDirectory, @Nonnull StateMachine stateMachine) {
        this.logDirectory = checkNotNull(logDirectory);
        this.stateMachine = checkNotNull(stateMachine);
    }

    @Override
    protected void configure() {

        bind(StateMachine.class).toInstance(stateMachine);
        bind(StateMachineProxy.class);
        bind(RaftLog.class).to(DefaultRaftLog.class).asEagerSingleton();
        expose(RaftLog.class);

    }

    @Nonnull
    @Provides
    @Singleton
    Journal getJournal() {

        try {

            /**
             * TODO Think more about what is really needed here.
             * This is really just the most basic configuration
             * possible. Specifically need to think through whether
             * we need a full sync to disk on every write. My suspicion
             * is yes. This configuration should probably be done by the
             * log.
             */
            final Journal journal = JournalBuilder.of(logDirectory).open();

            Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
                @Override
                public void run() {
                    //noinspection EmptyCatchBlock
                    try {
                        journal.close();
                    } catch (IOException e) {
                    }
                }
            }));

            return journal;

        } catch (IOException e) {

            throw propagate(e);

        }

    }

}
