/*-
 * *****
 * Concord
 * -----
 * Copyright (C) 2017 - 2018 Walmart Inc.
 * -----
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * =====
 */

import * as React from 'react';

import { Link, Redirect, Route, Switch } from 'react-router-dom';
import { Icon, Menu } from 'semantic-ui-react';

import { ConcordId } from '../../../api/common';
import { get as apiGet, isFinal, ProcessEntry } from '../../../api/process';
import { NotFoundPage } from '../../pages';
import {
    ProcessAnsibleActivitySwitcher,
    ProcessAttachmentsActivity,
    ProcessChildrenActivity,
    ProcessEventsActivity,
    ProcessHistoryActivity,
    ProcessLogActivity,
    ProcessStatusActivity,
    ProcessWaitActivity
} from '../index';
import ProcessToolbar from './Toolbar';
import { useCallback, useRef } from 'react';
import { usePolling } from '../../../api/usePolling';
import { useState } from 'react';
import RequestErrorActivity from '../RequestErrorActivity';

export type TabLink =
    | 'status'
    | 'ansible'
    | 'log'
    | 'events'
    | 'history'
    | 'wait'
    | 'children'
    | 'attachments'
    | null;

interface ExternalProps {
    instanceId: ConcordId;
    activeTab: TabLink;
}

const DATA_FETCH_INTERVAL = 5000;

const ProcessActivity = (props: ExternalProps) => {
    const stickyRef = useRef(null);

    const [loading, setLoading] = useState<boolean>(false);
    const loadingCounter = useRef<number>(0);
    const [refresh, toggleRefresh] = useState<boolean>(false);

    const loadingHandler = useCallback((inc: number) => {
        loadingCounter.current += inc;
        setLoading(loadingCounter.current > 0);
    }, []);

    const [process, setProcess] = useState<ProcessEntry>();

    const fetchData = useCallback(async () => {
        const process = await apiGet(props.instanceId, []);
        setProcess(process);
        return !isFinal(process.status);
    }, [props.instanceId]);

    const refreshHandler = useCallback(() => {
        toggleRefresh((prevState) => !prevState);
    }, []);

    const error = usePolling(fetchData, DATA_FETCH_INTERVAL, loadingHandler, refresh);

    if (error) {
        return <RequestErrorActivity error={error} />;
    }

    const { instanceId, activeTab } = props;

    const baseUrl = `/process/${instanceId}`;

    return (
        <div ref={stickyRef}>
            <ProcessToolbar
                loading={loading}
                instanceId={instanceId}
                process={process}
                refresh={refreshHandler}
                stickyRef={stickyRef}
            />

            <Menu tabular={true} style={{ marginTop: 0 }}>
                <Menu.Item active={activeTab === 'status'}>
                    <Icon name="hourglass half" />
                    <Link to={`${baseUrl}/status`}>Status</Link>
                </Menu.Item>
                <Menu.Item active={activeTab === 'events'}>
                    <Icon name="content" />
                    <Link to={`${baseUrl}/events`}>Events</Link>
                </Menu.Item>
                <Menu.Item active={activeTab === 'ansible'}>
                    <Icon name="chart area" />
                    <Link to={`${baseUrl}/ansible`}>Ansible</Link>
                </Menu.Item>
                <Menu.Item active={activeTab === 'log'}>
                    <Icon name="book" />
                    <Link to={`${baseUrl}/log`}>Logs</Link>
                </Menu.Item>
                <Menu.Item active={activeTab === 'history'}>
                    <Icon name="history" />
                    <Link to={`${baseUrl}/history`}>History</Link>
                </Menu.Item>
                <Menu.Item active={activeTab === 'wait'}>
                    <Icon name="wait" />
                    <Link to={`${baseUrl}/wait`}>Wait Conditions</Link>
                </Menu.Item>
                <Menu.Item active={activeTab === 'children'}>
                    <Icon name="chain" />
                    <Link to={`${baseUrl}/children`}>Child Processes</Link>
                </Menu.Item>
                <Menu.Item active={activeTab === 'attachments'}>
                    <Icon name="paperclip" />
                    <Link to={`${baseUrl}/attachments`}>Attachments</Link>
                </Menu.Item>
            </Menu>

            <Switch>
                <Route path={baseUrl} exact={true}>
                    <Redirect to={`${baseUrl}/status`} />
                </Route>
                <Route path={`${baseUrl}/status`}>
                    <ProcessStatusActivity
                        instanceId={instanceId}
                        loadingHandler={loadingHandler}
                        forceRefresh={refresh}
                        refreshHandler={refreshHandler}
                    />
                </Route>

                <Route path={`${baseUrl}/events`}>
                    <ProcessEventsActivity
                        instanceId={instanceId}
                        processStatus={process ? process.status : undefined}
                        loadingHandler={loadingHandler}
                        forceRefresh={refresh}
                    />
                </Route>
                <Route path={`${baseUrl}/ansible`}>
                    <ProcessAnsibleActivitySwitcher
                        instanceId={instanceId}
                        loadingHandler={loadingHandler}
                        forceRefresh={refresh}
                    />
                </Route>
                <Route path={`${baseUrl}/log`} exact={true}>
                    <ProcessLogActivity
                        instanceId={instanceId}
                        processStatus={process ? process.status : undefined}
                        loadingHandler={loadingHandler}
                        forceRefresh={refresh}
                    />
                </Route>
                <Route path={`${baseUrl}/history`} exact={true}>
                    <ProcessHistoryActivity
                        instanceId={instanceId}
                        loadingHandler={loadingHandler}
                        forceRefresh={refresh}
                    />
                </Route>
                <Route path={`${baseUrl}/wait`} exact={true}>
                    <ProcessWaitActivity
                        instanceId={instanceId}
                        processStatus={process ? process.status : undefined}
                        loadingHandler={loadingHandler}
                        forceRefresh={refresh}
                    />
                </Route>
                <Route path={`${baseUrl}/children`} exact={true}>
                    <ProcessChildrenActivity instanceId={instanceId} />
                </Route>
                <Route path={`${baseUrl}/attachments`} exact={true}>
                    <ProcessAttachmentsActivity
                        instanceId={instanceId}
                        processStatus={process ? process.status : undefined}
                        loadingHandler={loadingHandler}
                        forceRefresh={refresh}
                    />
                </Route>
                <Route component={NotFoundPage} />
            </Switch>
        </div>
    );
};

export default ProcessActivity;
