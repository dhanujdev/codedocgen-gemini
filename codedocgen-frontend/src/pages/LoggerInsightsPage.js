import React, { useState, useEffect } from 'react';
import { Table, TableBody, TableCell, TableContainer, TableHead, TableRow, Paper, Typography, CircularProgress, Alert, TextField, Button, Box, IconButton, Collapse, Switch, FormControlLabel, Select, MenuItem, FormControl, InputLabel } from '@mui/material';
import { KeyboardArrowDown, KeyboardArrowUp } from '@mui/icons-material';
import { jsPDF } from 'jspdf';
import 'jspdf-autotable';

const LogRow = ({ log, isInitiallyOpen }) => {
    const [open, setOpen] = useState(isInitiallyOpen);

    useEffect(() => {
        setOpen(isInitiallyOpen);
    }, [isInitiallyOpen]);

    const getRiskColor = (isRisk) => {
        return isRisk ? 'red' : 'inherit';
    };

    const getLevelColor = (level) => {
        switch (level?.toLowerCase()) {
            case 'error': return 'red';
            case 'warn': return 'orange';
            case 'info': return 'blue';
            case 'debug': return 'grey';
            case 'trace': return 'lightgrey';
            default: return 'inherit';
        }
    };

    return (
        <React.Fragment>
            <TableRow sx={{ '& > *': { borderBottom: 'unset' } }}>
                <TableCell>
                    <IconButton aria-label="expand row" size="small" onClick={() => setOpen(!open)}>
                        {open ? <KeyboardArrowUp /> : <KeyboardArrowDown />}
                    </IconButton>
                </TableCell>
                <TableCell component="th" scope="row" sx={{ color: getRiskColor(log.piiRisk || log.pciRisk) }}>
                    {log.className}
                </TableCell>
                <TableCell sx={{ color: getRiskColor(log.piiRisk || log.pciRisk) }}>{log.line}</TableCell>
                <TableCell sx={{ color: getLevelColor(log.level) }}>{log.level?.toUpperCase()}</TableCell>
                <TableCell sx={{ color: getRiskColor(log.piiRisk || log.pciRisk) }}>{log.message}</TableCell>
                <TableCell sx={{ color: getRiskColor(log.piiRisk) }}>{log.piiRisk ? 'Yes' : 'No'}</TableCell>
                <TableCell sx={{ color: getRiskColor(log.pciRisk) }}>{log.pciRisk ? 'Yes' : 'No'}</TableCell>
            </TableRow>
            <TableRow>
                <TableCell style={{ paddingBottom: 0, paddingTop: 0 }} colSpan={7}>
                    <Collapse in={open} timeout="auto" unmountOnExit>
                        <Box sx={{ margin: 1 }}>
                            <Typography variant="h6" gutterBottom component="div">
                                Variables
                            </Typography>
                            {log.variables && log.variables.length > 0 ? (
                                <Table size="small" aria-label="variables">
                                    <TableHead>
                                        <TableRow>
                                            <TableCell>Name</TableCell>
                                            <TableCell>Type</TableCell>
                                            <TableCell>PII</TableCell>
                                            <TableCell>PCI</TableCell>
                                        </TableRow>
                                    </TableHead>
                                    <TableBody>
                                        {log.variables.map((variable, index) => (
                                            <TableRow key={`${log.id}-var-${index}`} sx={{backgroundColor: (variable.pii || variable.pci) ? 'rgba(255, 0, 0, 0.1)' : 'inherit'}}>
                                                <TableCell>{variable.name}</TableCell>
                                                <TableCell>{variable.type}</TableCell>
                                                <TableCell>{variable.pii ? 'Yes' : 'No'}</TableCell>
                                                <TableCell>{variable.pci ? 'Yes' : 'No'}</TableCell>
                                            </TableRow>
                                        ))}
                                    </TableBody>
                                </Table>
                            ) : (
                                <Typography variant="body2">No variables logged or extracted for this statement.</Typography>
                            )}
                        </Box>
                    </Collapse>
                </TableCell>
            </TableRow>
        </React.Fragment>
    );
};

const LoggerInsightsPage = ({ analysisData, analysisLoading, analysisError, repoUrl }) => {
    const [allLogsFromAnalysis, setAllLogsFromAnalysis] = useState([]);
    const [filteredLogs, setFilteredLogs] = useState([]);
    const [searchTerm, setSearchTerm] = useState('');
    const [levelFilter, setLevelFilter] = useState('');
    const [piiFilter, setPiiFilter] = useState(false);
    const [pciFilter, setPciFilter] = useState(false);
    const [allRowsExpanded, setAllRowsExpanded] = useState(false);
    const [availableLevels, setAvailableLevels] = useState([]);

    useEffect(() => {
        if (analysisData && analysisData.logStatements) {
            const logs = analysisData.logStatements || [];
            setAllLogsFromAnalysis(logs);
            setFilteredLogs(logs);
            const levels = [...new Set(logs.map(log => log.level?.toUpperCase()).filter(Boolean))].sort();
            setAvailableLevels(levels);
        } else {
            setAllLogsFromAnalysis([]);
            setFilteredLogs([]);
            setAvailableLevels([]);
        }
    }, [analysisData]);

    useEffect(() => {
        let currentLogs = [...allLogsFromAnalysis];
        if (searchTerm) {
            currentLogs = currentLogs.filter(log => 
                log.className?.toLowerCase().includes(searchTerm.toLowerCase()) || 
                log.message?.toLowerCase().includes(searchTerm.toLowerCase())
            );
        }
        if (levelFilter) {
            currentLogs = currentLogs.filter(log => log.level?.toLowerCase() === levelFilter.toLowerCase());
        }
        if (piiFilter) {
            currentLogs = currentLogs.filter(log => log.piiRisk);
        }
        if (pciFilter) {
            currentLogs = currentLogs.filter(log => log.pciRisk);
        }
        setFilteredLogs(currentLogs);
    }, [searchTerm, levelFilter, piiFilter, pciFilter, allLogsFromAnalysis]);

    const downloadPdfReport = () => {
        const doc = new jsPDF();
        doc.text('Logger Insights Report', 14, 16);

        const tableColumn = ["Class/Filename", "Line", "Level", "Message", "PII Risk", "PCI Risk", "Variables"];
        const tableRows = [];

        filteredLogs.forEach(log => {
            const logData = [
                log.className,
                log.line,
                log.level?.toUpperCase(),
                log.message,
                log.piiRisk ? 'Yes' : 'No',
                log.pciRisk ? 'Yes' : 'No',
                log.variables && log.variables.length > 0 ? log.variables.map(v => `${v.name} (${v.type})${v.pii ? ' [PII]' : ''}${v.pci ? ' [PCI]' : ''}`).join(', ') : 'N/A'
            ];
            tableRows.push(logData);
        });

        doc.autoTable({
            head: [tableColumn],
            body: tableRows,
            startY: 20,
        });
        doc.save('logger-insights-report.pdf');
    };

    if (!repoUrl) {
        return <Alert severity="warning">Please select a repository and run the analysis first from the main page to see logger insights.</Alert>;
    }

    if (analysisLoading) {
        return <CircularProgress />;
    }

    if (analysisError) {
        return <Alert severity="error">An error occurred during analysis: {typeof analysisError === 'string' ? analysisError : analysisError.message || 'Unknown error'}. Please try again.</Alert>;
    }
    
    if (!analysisData || !analysisData.logStatements) {
        return <Alert severity="info">No logger insights data available. Please ensure the analysis has completed successfully.</Alert>;
    }

    return (
        <Paper sx={{ margin: 2, padding: 2 }}>
            <Typography variant="h4" gutterBottom>Logger Insights</Typography>
            
            <Box sx={{ display: 'flex', gap: 2, marginBottom: 2, alignItems: 'center', flexWrap: 'wrap' }}>
                <TextField 
                    label="Search Class/Message"
                    variant="outlined" 
                    size="small"
                    value={searchTerm}
                    onChange={(e) => setSearchTerm(e.target.value)}
                    sx={{ flexGrow: 1, minWidth: '150px' }}
                />
                <FormControl size="small" sx={{ minWidth: 150 }}>
                    <InputLabel id="level-filter-label">Level</InputLabel>
                    <Select
                        labelId="level-filter-label"
                        label="Level"
                        value={levelFilter}
                        onChange={(e) => setLevelFilter(e.target.value)}
                    >
                        <MenuItem value=""><em>All Levels</em></MenuItem>
                        {availableLevels.map(level => (
                            <MenuItem key={level} value={level.toLowerCase()}>{level}</MenuItem>
                        ))}
                    </Select>
                </FormControl>
                <FormControlLabel
                    control={<Switch checked={piiFilter} onChange={(e) => setPiiFilter(e.target.checked)} />}
                    label="PII Risk Only"
                />
                <FormControlLabel
                    control={<Switch checked={pciFilter} onChange={(e) => setPciFilter(e.target.checked)} />}
                    label="PCI Risk Only"
                />
                <Button variant="outlined" size="small" onClick={() => setAllRowsExpanded(true)} sx={{ height: 'fit-content' }}>Expand All</Button>
                <Button variant="outlined" size="small" onClick={() => setAllRowsExpanded(false)} sx={{ height: 'fit-content' }}>Collapse All</Button>
                <Button variant="contained" onClick={downloadPdfReport} sx={{ height: 'fit-content' }}>Download PDF</Button>
            </Box>

            <TableContainer component={Paper}>
                <Table aria-label="collapsible table">
                    <TableHead>
                        <TableRow>
                            <TableCell sx={{ width: '50px' }}>
                            </TableCell>
                            <TableCell>Class/Filename</TableCell>
                            <TableCell>Line No.</TableCell>
                            <TableCell>Level</TableCell>
                            <TableCell>Message</TableCell>
                            <TableCell>PII Risk</TableCell>
                            <TableCell>PCI Risk</TableCell>
                        </TableRow>
                    </TableHead>
                    <TableBody>
                        {filteredLogs.length > 0 ? (
                            filteredLogs.map((log) => (
                                <LogRow key={log.id || `${log.className}-${log.line}`} log={log} isInitiallyOpen={allRowsExpanded} />
                            ))
                        ) : (
                            <TableRow>
                                <TableCell colSpan={7} align="center">
                                    <Typography>No log statements found or matching your filters.</Typography>
                                </TableCell>
                            </TableRow>
                        )}
                    </TableBody>
                </Table>
            </TableContainer>
            {filteredLogs.length === 0 && allLogsFromAnalysis.length > 0 && (
                 <Alert severity="info" sx={{marginTop: 2}}>No logs match your current filter criteria, but other logs were found.</Alert>
            )}
            {allLogsFromAnalysis.length === 0 && !analysisLoading && (
                <Alert severity="info" sx={{marginTop: 2}}>No log statements were found in the analyzed project.</Alert>
            )}
        </Paper>
    );
};

export default LoggerInsightsPage; 