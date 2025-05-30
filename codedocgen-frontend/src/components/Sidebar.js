import React from 'react';
import { Box, List, ListItemButton, ListItemIcon, ListItemText, Typography } from '@mui/material';
import HomeIcon from '@mui/icons-material/Home';
import ApiIcon from '@mui/icons-material/Api';
import ListAltIcon from '@mui/icons-material/ListAlt';
import CategoryIcon from '@mui/icons-material/Category';
import SchemaIcon from '@mui/icons-material/Schema';
import PublishIcon from '@mui/icons-material/Publish';
import ArticleIcon from '@mui/icons-material/Article';
import AccountTreeIcon from '@mui/icons-material/AccountTree';
import ClassIcon from '@mui/icons-material/Class';
import StorageIcon from '@mui/icons-material/Storage';
import PolicyIcon from '@mui/icons-material/Policy';
import SecurityIcon from '@mui/icons-material/Security';

const NAV_ITEMS = [
  { label: 'Home', icon: <HomeIcon />, key: 'home' },
  { label: 'Overview', icon: <HomeIcon />, key: 'overview' },
  { label: 'Endpoints', icon: <ApiIcon />, key: 'endpoints' },
  { label: 'API Specs', icon: <ArticleIcon />, key: 'apiSpecs' },
  { label: 'Call Flow', icon: <AccountTreeIcon />, key: 'callFlow' },
  { label: 'Features', icon: <ListAltIcon />, key: 'features' },
  { label: 'Entities', icon: <CategoryIcon />, key: 'entities' },
  { label: 'Database', icon: <StorageIcon />, key: 'database' },
  { label: 'All Classes', icon: <ClassIcon />, key: 'allClasses' },
  { label: 'Diagrams', icon: <SchemaIcon />, key: 'diagrams' },
  { label: 'Logger Insights', icon: <PolicyIcon />, key: 'loggerInsights' },
  { label: 'PCI/PII Scan', icon: <SecurityIcon />, key: 'piiPciScan' },
  { label: 'Publish', icon: <PublishIcon />, key: 'publish' },
];

function Sidebar({ activeSection, setActiveSection }) {
  return (
    <Box sx={{ width: 240, bgcolor: '#f4f6fa', borderRight: '1px solid #e0e0e0', py: 3, px: 0, display: 'flex', flexDirection: 'column', alignItems: 'center', boxShadow: 1, minHeight: '100vh' }}>
      <Box sx={{ mb: 3, display: 'flex', flexDirection: 'column', alignItems: 'center' }}>
        <Typography variant="h5" sx={{ fontWeight: 700, color: '#4f46e5', fontFamily: 'Quicksand', mb: 0.5 }}>CodeDocGen</Typography>
        <Typography variant="caption" sx={{ color: '#888', fontFamily: 'Quicksand' }}>Documentation Dashboard</Typography>
      </Box>
      <List sx={{ width: '100%', mt: 1 }}>
        {NAV_ITEMS.map((item) => (
          <ListItemButton
            key={item.key}
            selected={activeSection === item.key}
            onClick={() => setActiveSection(item.key)}
            sx={{ borderRadius: 2, mb: 0.5, mx: 1, bgcolor: activeSection === item.key ? '#e0e7ff' : 'inherit', color: activeSection === item.key ? '#4f46e5' : '#333', transition: 'all 0.2s', fontFamily: 'Quicksand' }}
          >
            <ListItemIcon sx={{ color: activeSection === item.key ? '#4f46e5' : '#888' }}>{item.icon}</ListItemIcon>
            <ListItemText primary={item.label} />
          </ListItemButton>
        ))}
      </List>
    </Box>
  );
}

export default Sidebar; 