import React, { useState, useEffect } from 'react';
import { Box, Typography, Paper, Table, TableBody, TableCell, TableContainer, TableHead, TableRow, TextField, InputAdornment, IconButton, Collapse, Button, Grid, Chip } from '@mui/material';
import SearchIcon from '@mui/icons-material/Search';
import ExpandMoreIcon from '@mui/icons-material/ExpandMore';
import ExpandLessIcon from '@mui/icons-material/ExpandLess';

function PiiPciScanPage({ analysisData }) {
    // console.log('[PiiPciScanPage] Received analysisData:', analysisData); // Log 1

    const [searchTerm, setSearchTerm] = useState('');
    const [findings, setFindings] = useState([]);
    const [filteredFindings, setFilteredFindings] = useState([]);
    const [expandedRows, setExpandedRows] = useState({});

    useEffect(() => {
        if (analysisData && analysisData.piiPciFindings) {
            // console.log('[PiiPciScanPage] Setting findings from analysisData.piiPciFindings:', analysisData.piiPciFindings); // Log 2
            setFindings(analysisData.piiPciFindings);
            setFilteredFindings(analysisData.piiPciFindings);
        } else {
            // console.log('[PiiPciScanPage] analysisData.piiPciFindings is missing or undefined'); // Log 3
            setFindings([]);
            setFilteredFindings([]);
        }
    }, [analysisData]);

    // useEffect(() => { // Log 4: To see current findings state
    //     console.log('[PiiPciScanPage] Current findings state:', findings);
    // }, [findings]);

    useEffect(() => {
        const lowercasedFilter = searchTerm.toLowerCase();
        const filteredData = findings.filter(item => {
            return (
                item.filePath.toLowerCase().includes(lowercasedFilter) ||
                item.findingType.toLowerCase().includes(lowercasedFilter) ||
                item.matchedText.toLowerCase().includes(lowercasedFilter)
            );
        });
        setFilteredFindings(filteredData);
        // console.log('[PiiPciScanPage] Filtered findings state:', filteredData); // Log 5
    }, [searchTerm, findings]);

    const handleSearchChange = (event) => {
        setSearchTerm(event.target.value);
    };

    const toggleRow = (id) => {
        setExpandedRows(prev => ({ ...prev, [id]: !prev[id] }));
    };

    const expandAll = () => {
        const newExpandedRows = {};
        filteredFindings.forEach((_, index) => newExpandedRows[index] = true);
        setExpandedRows(newExpandedRows);
    };

    const collapseAll = () => {
        setExpandedRows({});
    };

    // Improved check for initial render or no data
    if (!analysisData) {
        return <Typography sx={{ p: 3 }}>Loading analysis data or no analysis performed yet...</Typography>;
    }

    if (!analysisData.piiPciFindings || analysisData.piiPciFindings.length === 0) {
        return <Typography sx={{ p: 3 }}>No PCI/PII scan data available. Please perform an analysis or check scanner configuration.</Typography>;
    }

    return (
        <Box sx={{ p: 3 }}>
            <Typography variant="h4" gutterBottom sx={{ fontFamily: 'Quicksand', fontWeight: 700, color: '#333' }}>
                PCI/PII Scan Results
            </Typography>

            <Paper sx={{ p: 2, mb: 3, borderRadius: 2, boxShadow: '0 1px 3px rgba(0,0,0,0.12), 0 1px 2px rgba(0,0,0,0.24)' }}>
                <Grid container spacing={2} alignItems="center">
                    <Grid item xs={12} md={8}>
                        <TextField
                            fullWidth
                            variant="outlined"
                            label="Search by File Path, Finding Type, or Matched Text"
                            value={searchTerm}
                            onChange={handleSearchChange}
                            InputProps={{
                                startAdornment: (
                                    <InputAdornment position="start">
                                        <SearchIcon />
                                    </InputAdornment>
                                ),
                            }}
                            sx={{mr: 2}}
                        />
                    </Grid>
                    <Grid item xs={12} md={4} sx={{ display: 'flex', justifyContent: { xs: 'flex-start', md: 'flex-end' } }}>
                        <Button onClick={expandAll} variant="outlined" sx={{ mr: 1 }}>Expand All</Button>
                        <Button onClick={collapseAll} variant="outlined">Collapse All</Button>
                    </Grid>
                </Grid>
            </Paper>

            {filteredFindings.length > 0 ? (
                <TableContainer component={Paper} sx={{ borderRadius: 2, boxShadow: '0 1px 3px rgba(0,0,0,0.12), 0 1px 2px rgba(0,0,0,0.24)' }}>
                    <Table stickyHeader aria-label="pii pci findings table">
                        <TableHead>
                            <TableRow>
                                <TableCell sx={{ fontWeight: 'bold', bgcolor: '#f4f6fa' }}>File Path</TableCell>
                                <TableCell sx={{ fontWeight: 'bold', bgcolor: '#f4f6fa' }}>Line</TableCell>
                                <TableCell sx={{ fontWeight: 'bold', bgcolor: '#f4f6fa' }}>Column</TableCell>
                                <TableCell sx={{ fontWeight: 'bold', bgcolor: '#f4f6fa' }}>Finding Type</TableCell>
                                <TableCell sx={{ fontWeight: 'bold', bgcolor: '#f4f6fa' }}>Matched Text (Preview)</TableCell>
                                <TableCell sx={{ fontWeight: 'bold', bgcolor: '#f4f6fa', width: '5%' }}></TableCell>
                            </TableRow>
                        </TableHead>
                        <TableBody>
                            {filteredFindings.map((finding, index) => (
                                <React.Fragment key={index}>
                                    <TableRow hover sx={{ '&:last-child td, &:last-child th': { border: 0 } }}>
                                        <TableCell>{finding.filePath}</TableCell>
                                        <TableCell>{finding.lineNumber}</TableCell>
                                        <TableCell>{finding.columnNumber}</TableCell>
                                        <TableCell>
                                            <Chip 
                                                label={finding.findingType}
                                                color={finding.findingType.startsWith('PII') ? "error" : (finding.findingType.startsWith('PCI') ? "warning" : "default")}
                                                size="small"
                                            />
                                        </TableCell>
                                        <TableCell>{finding.matchedText.substring(0, 50)}{finding.matchedText.length > 50 ? '...' : ''}</TableCell>
                                        <TableCell>
                                            <IconButton onClick={() => toggleRow(index)} size="small">
                                                {expandedRows[index] ? <ExpandLessIcon /> : <ExpandMoreIcon />}
                                            </IconButton>
                                        </TableCell>
                                    </TableRow>
                                    <TableRow>
                                        <TableCell style={{ paddingBottom: 0, paddingTop: 0 }} colSpan={6}>
                                            <Collapse in={expandedRows[index]} timeout="auto" unmountOnExit>
                                                <Box sx={{ margin: 1, p: 2, bgcolor: '#fafafa', borderRadius: 1 }}>
                                                    <Typography variant="subtitle2" gutterBottom component="div" sx={{ fontWeight: 'bold' }}>
                                                        Full Matched Text:
                                                    </Typography>
                                                    <Typography variant="body2" sx={{ whiteSpace: 'pre-wrap', wordBreak: 'break-all', fontFamily: 'monospace'}}>
                                                        {finding.matchedText}
                                                    </Typography>
                                                </Box>
                                            </Collapse>
                                        </TableCell>
                                    </TableRow>
                                </React.Fragment>
                            ))}
                        </TableBody>
                    </Table>
                </TableContainer>
            ) : (
                 // Check if original findings also had 0 length to differentiate between no data and filtered to empty
                findings.length === 0 ? 
                <Typography sx={{ p: 3, textAlign: 'center' }}>No PCI/PII findings were returned from the analysis.</Typography> :
                <Typography sx={{ p: 3, textAlign: 'center' }}>No PCI/PII findings matching your current filter criteria.</Typography>
            )}
        </Box>
    );
}

export default PiiPciScanPage; 